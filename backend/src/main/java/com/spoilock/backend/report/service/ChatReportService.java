package com.spoilock.backend.report.service;

import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.chat.service.ChatRoomAccessService;
import com.spoilock.backend.report.dto.ChatReportRequest;
import com.spoilock.backend.report.dto.ChatReportResponse;
import com.spoilock.backend.report.entity.ChatReport;
import com.spoilock.backend.report.event.ChatReportSubmitted;
import com.spoilock.backend.report.repository.ChatReportRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatReportService {
    private final ChatReportRepository chatReportRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatRoomAccessService chatRoomAccessService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatReportResponse submit(
            Long roomId,
            Long messageId,
            Long reporterId,
            ChatReportRequest request
    ){
        chatRoomAccessService.checkReadAccess(roomId, reporterId);

        //같은 방의 신고저장 처리
        ChatRoom room = entityManager.find(
                ChatRoom.class, roomId, LockModeType.PESSIMISTIC_WRITE);

        if (room == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "채팅방을 찾을 수 없습니다."
            );
        }

        //잠금을 기다리는 동안 권한이 바뀌었는지 재확인
        chatRoomAccessService.checkReadAccess(roomId, reporterId);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "신고할 메시지를 찾을 수 없습니다."
                ));
        if (!message.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이 채팅방의 메시지가 아닙니다."
            );
        }
        if (message.getMessageType() != MessageType.CHAT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "입장·퇴장 등의 시스템 알림은 신고할 수 없습니다."
            );
        }

        Long targetUserId = message.getUser().getId();

        if (targetUserId.equals(reporterId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자신의 메시지는 신고할 수 없습니다."
            );
        }

        if (chatReportRepository.existsByMessageAndReporter(
                messageId,
                reporterId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 신고한 메시지입니다."
            );
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 정보를 확인해 주세요."
                ));

        long previousCount = chatReportRepository.countPendingByRoomAndTarget(
                roomId, targetUserId);
        ChatReport report = ChatReport.create(
                message,
                reporter,
                request.reason(),
                request.detail()
        );

        ChatReport saved = chatReportRepository.saveAndFlush(report);

        //미처리 신고가 2건 > 3건이 되는 순간
        boolean thresholdReached = previousCount == 2;

        eventPublisher.publishEvent(
                new ChatReportSubmitted(
                        saved.getId(),
                        roomId,
                        targetUserId,
                        thresholdReached
                )
        );
        return new ChatReportResponse(
                saved.getId(),
                "신고가 접수되었습니다."
        );
    }

}

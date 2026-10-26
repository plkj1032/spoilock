package com.spoilock.backend.report.service;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.chat.service.ChatService;
import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.domain.type.NotificationType;
import com.spoilock.backend.notification.repository.NotificationRepository;
import com.spoilock.backend.report.dto.ChatReportDecisionRequest;
import com.spoilock.backend.report.dto.ChatReportReviewResponse;
import com.spoilock.backend.report.entity.ChatReport;
import com.spoilock.backend.report.enums.ReportStatus;
import com.spoilock.backend.report.repository.ChatReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatReportDecisionService {

    private final NotificationRepository notificationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatReportRepository chatReportRepository;
    private final ChatReportReviewService chatReportReviewService;
    private final ChatService chatService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatRedisEvent decide(
            Long notificationId,
            Long userId,
            ChatReportDecisionRequest request
    ) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .filter(item -> item.getReceiverId().equals(userId))
                .filter(item ->
                        item.getType()
                                == NotificationType.CHAT_REPORT_THRESHOLD
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "신고 검토 알림을 찾을 수 없습니다."
                ));

        // 신고 접수·다른 관리자 처리·일반 강퇴와 같은 방 잠금 사용
        ChatRoom room = chatRoomRepository
                .findOneById(notification.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        // 잠금 획득 후 현재 관리 권한과 처리 상태 재확인
        ChatReportReviewResponse review =
                chatReportReviewService.getReview(
                        notificationId,
                        userId
                );

        if (review.processed()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 처리된 신고입니다. 목록을 다시 확인해 주세요."
            );
        }

        if (room.getCreatedBy().getId().equals(review.targetUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "방장에 대한 신고는 처리할 수 없습니다."
            );
        }

        List<ChatReport> reports =
                chatReportRepository.findPendingByRoomAndTarget(
                        room.getId(),
                        review.targetUserId()
                );

        Set<Long> currentIds = reports.stream()
                .map(ChatReport::getId)
                .collect(Collectors.toSet());

        Set<Long> requestedIds = new HashSet<>(request.reportIds());

        if (currentIds.isEmpty()
                || requestedIds.size() != request.reportIds().size()
                || !currentIds.equals(requestedIds)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "신고 목록이 변경되었습니다. 다시 확인해 주세요."
            );
        }

        ChatRedisEvent kickEvent = null;
        ReportStatus result;

        if (request.decision()
                == ChatReportDecisionRequest.Decision.KICK) {
            kickEvent = chatService.kickMember(
                    room.getId(),
                    userId,
                    review.targetUserId(),
                    "신고 검토 후 방 관리자가 강퇴했습니다."
            );

            result = ReportStatus.APPROVED;
        } else {
            result = ReportStatus.REJECTED;
        }

        for (ChatReport report : reports) {
            report.process(result);
        }

        notification.markAsRead();

        // 강퇴나 신고 처리 중 실패하면 전체 트랜잭션 롤백
        return kickEvent;
    }
}
package com.spoilock.backend.report.service;

import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.service.ChatRoomAccessService;
import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.domain.type.NotificationType;
import com.spoilock.backend.notification.repository.NotificationRepository;
import com.spoilock.backend.report.dto.ChatReportReviewResponse;
import com.spoilock.backend.report.entity.ChatReport;
import com.spoilock.backend.report.enums.ReportStatus;
import com.spoilock.backend.report.repository.ChatReportRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReportReviewService {

    private final NotificationRepository notificationRepository;
    private final ChatReportRepository chatReportRepository;
    private final UserRepository userRepository;
    private final ChatRoomAccessService chatRoomAccessService;

    public ChatReportReviewResponse getReview(
            Long notificationId,
            Long userId
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

        ChatReport sourceReport = chatReportRepository
                .findById(notification.getSourceReportId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "신고 정보를 찾을 수 없습니다."
                ));

        ChatRoom room = sourceReport.getMessage().getRoom();
        User target = sourceReport.getMessage().getUser();

        if (!room.getId().equals(notification.getRoomId())
                || !target.getId().equals(
                notification.getTargetUserId()
        )) {
            throw new IllegalStateException(
                    "알림과 신고 정보가 일치하지 않습니다."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 정보를 확인해 주세요."
                ));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "이용 가능한 계정이 아닙니다."
            );
        }

        // 알림 수신 당시가 아닌 현재의 방 관리 권한 확인
        chatRoomAccessService.checkKickPermission(room, user);

        // 피신고자 본인이 자신의 신고를 검토·처리하지 못하게 함
        if (target.getId().equals(userId)) {
            throw new AccessDeniedException(
                    "본인에 대한 신고는 직접 검토할 수 없습니다."
            );
        }

        boolean processed =
                sourceReport.getStatus() != ReportStatus.PENDING;

        List<ChatReportReviewResponse.ReportItem> reports;

        if (processed) {
            // 예전 알림으로 이후 새 신고까지 처리하지 않도록 구분
            reports = List.of();
        } else {
            reports = chatReportRepository
                    .findPendingByRoomAndTarget(
                            room.getId(),
                            target.getId()
                    )
                    .stream()
                    .map(report ->
                            new ChatReportReviewResponse.ReportItem(
                                    report.getId(),
                                    report.getReason(),
                                    report.getReason().getLabel(),
                                    report.getDetail(),
                                    report.getCreatedAt(),
                                    report.getMessage().getId(),
                                    report.getMessage().getContent(),
                                    report.getMessage().getCreatedAt()
                            )
                    )
                    .toList();
        }

        String roomName = room.getRoomType() == ChatRoomType.DEFAULT
                ? room.getDrama().getTitle() + " 토론방"
                : room.getRoomName();

        return new ChatReportReviewResponse(
                notification.getId(),
                room.getId(),
                roomName,
                target.getId(),
                target.getNickname(),
                processed,
                reports
        );
    }
}
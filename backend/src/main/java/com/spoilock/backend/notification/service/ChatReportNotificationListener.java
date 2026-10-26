package com.spoilock.backend.notification.service;

import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.domain.type.NotificationType;
import com.spoilock.backend.notification.repository.NotificationRepository;
import com.spoilock.backend.report.entity.ChatReport;
import com.spoilock.backend.report.event.ChatReportSubmitted;
import com.spoilock.backend.report.repository.ChatReportRecipientRepository;
import com.spoilock.backend.report.repository.ChatReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatReportNotificationListener {

    private final ChatReportRepository chatReportRepository;
    private final ChatReportRecipientRepository recipientRepository;
    private final NotificationRepository notificationRepository;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(ChatReportSubmitted event) {
        ChatReport report = chatReportRepository.findById(event.reportId())
                .orElseThrow(() -> new IllegalStateException(
                        "알림을 생성할 신고를 찾을 수 없습니다."
                ));

        ChatRoom room = report.getMessage().getRoom();
        Long targetUserId = report.getMessage().getUser().getId();

        String roomName = getRoomName(room);

        // 1. 신고 접수마다 피신고자 알림 생성
        // 신고자 이름, ID, 상세 설명, 원문은 포함하지 않는다.
        String targetContent = "“" + roomName + "”에서 "
                + report.getReason().getLabel()
                + " 사유로 회원님에 대한 신고가 접수되었습니다.";

        notificationRepository.save(
                Notification.create(
                        targetUserId,
                        NotificationType.CHAT_REPORT_RECEIVED,
                        room.getId(),
                        targetUserId,
                        report.getId(),
                        targetContent
                )
        );

        // 관리자 알림은 미처리 신고가 3건에 도달할 때만 생성
        if (!event.thresholdReached()) {
            return;
        }

        List<ChatReport> pendingReports =
                chatReportRepository.findPendingByRoomAndTarget(
                        room.getId(),
                        targetUserId
                );

        String targetNickname =
                report.getMessage().getUser().getNickname();

        String managerContent = buildManagerContent(
                roomName,
                targetNickname,
                pendingReports
        );

        List<Long> receiverIds = switch (room.getRoomType()) {
            case DEFAULT -> recipientRepository.findActiveAdminIds();
            case CUSTOM, PRIVATE -> List.of(room.getCreatedBy().getId());
        };

        for (Long receiverId : receiverIds) {
            notificationRepository.save(
                    Notification.create(
                            receiverId,
                            NotificationType.CHAT_REPORT_THRESHOLD,
                            room.getId(),
                            targetUserId,
                            report.getId(),
                            managerContent
                    )
            );
        }
    }

    private String getRoomName(ChatRoom room) {
        if (room.getRoomType() == ChatRoomType.DEFAULT) {
            return room.getDrama().getTitle() + " 토론방";
        }

        return room.getRoomName();
    }

    private String buildManagerContent(
            String roomName,
            String targetNickname,
            List<ChatReport> reports
    ) {
        StringBuilder content = new StringBuilder();

        content.append("“")
                .append(roomName)
                .append("”에서 ")
                .append(targetNickname)
                .append("님에 대한 미처리 신고가 ")
                .append(reports.size())
                .append("건 접수되었습니다.\n\n");

        for (int i = 0; i < reports.size(); i++) {
            content.append(i + 1)
                    .append(". ")
                    .append(reports.get(i).getReason().getLabel())
                    .append("\n");
        }

        content.append("\n신고 내용을 확인하고 강퇴 여부를 결정해 주세요.");

        return content.toString();
    }
}
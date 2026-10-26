package com.spoilock.backend.notification.domain.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.notification.domain.type.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_receiver_report_type",
                        columnNames = {
                                "receiver_id",
                                "source_report_id",
                                "notification_type"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림을 받는 회원
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_type",
            nullable = false,
            length = 30
    )
    private NotificationType type;

    // 신고가 발생한 방
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // 피신고자
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    // 이 알림을 발생시킨 신고
    @Column(name = "source_report_id", nullable = false)
    private Long sourceReportId;

    // 알림 생성 당시의 안내 문구
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    // null이면 안 읽은 알림
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification create(
            Long receiverId,
            NotificationType type,
            Long roomId,
            Long targetUserId,
            Long sourceReportId,
            String content
    ) {
        if (content == null
                || content.isBlank()
                || content.length() > 1000) {
            throw new IllegalArgumentException(
                    "알림 내용은 1자 이상 1000자 이하로 입력해 주세요."
            );
        }

        Notification notification = new Notification();

        notification.receiverId = Objects.requireNonNull(receiverId);
        notification.type = Objects.requireNonNull(type);
        notification.roomId = Objects.requireNonNull(roomId);
        notification.targetUserId = Objects.requireNonNull(targetUserId);
        notification.sourceReportId = Objects.requireNonNull(sourceReportId);
        notification.content = content;

        return notification;
    }

    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}
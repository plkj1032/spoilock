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
                ),
                @UniqueConstraint(
                        name = "uk_notification_receiver_invitation_type",
                        columnNames = {
                                "receiver_id",
                                "invitation_id",
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

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_type",
            nullable = false,
            length = 30
    )
    private NotificationType type;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "source_report_id")
    private Long sourceReportId;

    @Column(name = "invitation_id")
    private Long invitationId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    // 기존 신고 알림 생성 메서드
    public static Notification create(
            Long receiverId,
            NotificationType type,
            Long roomId,
            Long targetUserId,
            Long sourceReportId,
            String content
    ) {
        if (type != NotificationType.CHAT_REPORT_RECEIVED
                && type != NotificationType.CHAT_REPORT_THRESHOLD) {
            throw new IllegalArgumentException(
                    "신고 알림 타입만 사용할 수 있습니다."
            );
        }

        Notification notification = base(
                receiverId,
                type,
                content
        );

        notification.roomId = Objects.requireNonNull(roomId);
        notification.targetUserId = Objects.requireNonNull(targetUserId);
        notification.sourceReportId = Objects.requireNonNull(sourceReportId);

        // 신고자의 신원은 알림 발신자로 저장하지 않는다.
        return notification;
    }

    public static Notification createFollow(
            Long receiverId,
            Long senderUserId,
            String senderNickname
    ) {
        validateSender(receiverId, senderUserId);
        requireText(senderNickname, "발신자 닉네임");

        Notification notification = base(
                receiverId,
                NotificationType.FOLLOW_RECEIVED,
                senderNickname + "님이 회원님을 팔로우했습니다."
        );

        notification.senderUserId = senderUserId;

        return notification;
    }

    // 실제 초대 기록 생성 후 초대 서비스에서 호출
    public static Notification createInvitation(
            Long receiverId,
            Long senderUserId,
            String senderNickname,
            Long roomId,
            String roomName,
            Long invitationId
    ) {
        validateSender(receiverId, senderUserId);
        requireText(senderNickname, "발신자 닉네임");
        requireText(roomName, "채팅방 이름");

        Notification notification = base(
                receiverId,
                NotificationType.CHAT_ROOM_INVITATION,
                senderNickname + "님이 ‘" + roomName
                        + "’ 채팅방에 초대했습니다."
        );

        notification.senderUserId = senderUserId;
        notification.roomId = Objects.requireNonNull(roomId);
        notification.invitationId = Objects.requireNonNull(invitationId);

        return notification;
    }

    public static Notification createPostComment(
            Long receiverId,
            Long senderUserId,
            String senderNickname
    ) {
        validateSender(receiverId, senderUserId);
        requireText(senderNickname, "발신자 닉네임");

        Notification notification = base(
                receiverId,
                NotificationType.POST_COMMENT_RECEIVED,
                senderNickname + "님이 회원님의 게시글에 댓글을 남겼습니다."
        );

        notification.senderUserId = senderUserId;

        return notification;
    }

    public static Notification createPostLike(
            Long receiverId,
            Long senderUserId,
            String senderNickname
    ) {
        validateSender(receiverId, senderUserId);
        requireText(senderNickname, "발신자 닉네임");

        Notification notification = base(
                receiverId,
                NotificationType.POST_LIKE_RECEIVED,
                senderNickname + "님이 회원님의 게시글을 좋아합니다."
        );

        notification.senderUserId = senderUserId;

        return notification;
    }

    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    // 원본 신고·초대 기록은 유지하고 알림창에서만 숨김
    public void dismiss() {
        if (dismissedAt == null) {
            dismissedAt = LocalDateTime.now();
        }
    }

    private static Notification base(
            Long receiverId,
            NotificationType type,
            String content
    ) {
        requireText(content, "알림 내용");

        if (content.length() > 1000) {
            throw new IllegalArgumentException(
                    "알림 내용은 1000자 이하로 입력해 주세요."
            );
        }

        Notification notification = new Notification();

        notification.receiverId = Objects.requireNonNull(receiverId);
        notification.type = Objects.requireNonNull(type);
        notification.content = content;

        return notification;
    }

    private static void validateSender(
            Long receiverId,
            Long senderUserId
    ) {
        Objects.requireNonNull(receiverId);
        Objects.requireNonNull(senderUserId);

        if (receiverId.equals(senderUserId)) {
            throw new IllegalArgumentException(
                    "자기 자신에게 이 알림을 보낼 수 없습니다."
            );
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + "이 필요합니다."
            );
        }
    }
}
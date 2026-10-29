package com.spoilock.backend.chat.domain.entity;

import com.spoilock.backend.chat.domain.type.ChatInvitationStatus;
import com.spoilock.backend.common.entity.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "chat_room_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_invitation_code_hash",
                columnNames = "code_hash"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomInvitation extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(
            name = "code_hash",
            nullable = false,
            length = 64
    )
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatInvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public static ChatRoomInvitation create(
            Long roomId,
            Long senderUserId,
            Long receiverUserId,
            String codeHash,
            LocalDateTime now
    ) {
        Objects.requireNonNull(roomId);
        Objects.requireNonNull(senderUserId);
        Objects.requireNonNull(receiverUserId);
        Objects.requireNonNull(now);

        if (senderUserId.equals(receiverUserId)) {
            throw new IllegalArgumentException(
                    "자기 자신을 초대할 수 없습니다."
            );
        }

        if (codeHash == null || !codeHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "올바른 초대코드 해시가 필요합니다."
            );
        }

        ChatRoomInvitation invitation = new ChatRoomInvitation();

        invitation.roomId = roomId;
        invitation.senderUserId = senderUserId;
        invitation.receiverUserId = receiverUserId;
        invitation.codeHash = codeHash;
        invitation.status = ChatInvitationStatus.PENDING;
        invitation.expiresAt = now.plusHours(24);

        return invitation;
    }

    public boolean isPendingAndValid(LocalDateTime now) {
        return status == ChatInvitationStatus.PENDING
                && expiresAt.isAfter(now);
    }

    public void accept(LocalDateTime now) {
        requirePendingAndValid(now);
        status = ChatInvitationStatus.ACCEPTED;
        respondedAt = now;
    }

    public void reject(LocalDateTime now) {
        requirePendingAndValid(now);
        status = ChatInvitationStatus.REJECTED;
        respondedAt = now;
    }

    public void cancel(LocalDateTime now) {
        if (status != ChatInvitationStatus.PENDING) {
            throw new IllegalStateException(
                    "이미 처리된 초대입니다."
            );
        }

        status = ChatInvitationStatus.CANCELLED;
        respondedAt = now;
    }

    private void requirePendingAndValid(LocalDateTime now) {
        if (!isPendingAndValid(now)) {
            throw new IllegalStateException(
                    "만료되었거나 이미 처리된 초대입니다."
            );
        }
    }
}
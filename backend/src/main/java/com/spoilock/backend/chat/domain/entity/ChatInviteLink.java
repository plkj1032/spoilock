package com.spoilock.backend.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name="chat_invite_links")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class ChatInviteLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "issuer_id", nullable = false)
    private Long issuerId;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    public static ChatInviteLink create(
            Long roomId,
            Long issuerId,
            String codeHash,
            int maxUses,
            LocalDateTime now
    ) {
        Objects.requireNonNull(roomId);
        Objects.requireNonNull(issuerId);
        Objects.requireNonNull(now);

        if (codeHash == null || !codeHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "올바른 초대코드 해시가 필요합니다."
            );
        }

        if (maxUses < 1 || maxUses > 100) {
            throw new IllegalArgumentException(
                    "초대 가능 인원은 1~100명입니다."
            );
        }

        ChatInviteLink link = new ChatInviteLink();

        link.roomId = roomId;
        link.issuerId = issuerId;
        link.codeHash = codeHash;
        link.expiresAt = now.plusHours(24);
        link.revoked = false;
        link.maxUses = maxUses;
        link.usedCount = 0;

        return link;
    }

    public void revoke() {
        revoked = true;
    }

    public void consume() {
        if (usedCount >= maxUses) {
            throw new IllegalStateException(
                    "초대 가능 인원을 모두 사용했습니다."
            );
        }

        usedCount++;
    }
}
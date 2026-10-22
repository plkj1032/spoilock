
package com.spoilock.backend.user.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshSession extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash",
            nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static RefreshSession create(
            User user,
            String tokenHash,
            LocalDateTime expiresAt
    )
    {
        RefreshSession session = new RefreshSession();
        session.user = user;
        session.tokenHash = tokenHash;
        session.expiresAt = expiresAt;
        return session;
    }

    public boolean isUsable(LocalDateTime now)
    {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime now)
    {
        this.revokedAt = now;
    }
}

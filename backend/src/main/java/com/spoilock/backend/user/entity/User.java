
package com.spoilock.backend.user.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.user.enums.UserRole;
import com.spoilock.backend.user.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status",
            nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

    public static User createLocal(
            String email,
            String passwordHash,
            String nickname
    )
    {
        User user = new User();

        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;

        return user;
    }

    public void incrementTokenVersion()
    {
        this.tokenVersion++;
    }

    public static User createKakao(String nickname) {
        User user = new User();

        user.email = null;
        user.passwordHash = null;
        user.nickname = nickname;

        return user;
    }
    public void updateNickname(String nickname) {
        if (nickname == null) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }

        String normalized = nickname.trim();

        if (normalized.length() < 2 || normalized.length() > 20) {
            throw new IllegalArgumentException(
                    "닉네임은 2~20자로 입력해주세요."
            );
        }

        this.nickname = normalized;
    }
}

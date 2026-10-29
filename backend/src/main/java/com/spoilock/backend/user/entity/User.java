
package com.spoilock.backend.user.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.user.enums.UserRole;
import com.spoilock.backend.user.enums.AccountStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

    // V10: DB에서 자동 계산하는 닉네임 정규화 값
    @Column(
            name = "nickname_key",
            length = 50,
            insertable = false,
            updatable = false
    )
    private String nicknameKey;

    // V10: 이메일 소유 확인 완료 시각
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_status",
            nullable = false,
            length = 20
    )
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

    // 이메일 인증이 완료된 일반 회원 생성
    // 반드시 AuthService에서 signupProof 검증 후 호출
    public static User createLocal(
            String email,
            String passwordHash,
            String nickname
    ) {
        User user = new User();

        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.emailVerifiedAt =
                LocalDateTime.now(ZoneOffset.UTC);

        return user;
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    // 카카오 회원은 이메일 인증과 별도로 관리
    public static User createKakao(String nickname) {
        User user = new User();

        user.email = null;
        user.passwordHash = null;
        user.nickname = nickname;

        return user;
    }

    public void updateNickname(String nickname) {
        if (nickname == null) {
            throw new IllegalArgumentException(
                    "닉네임을 입력해주세요."
            );
        }

        String normalized = nickname.trim();

        if (normalized.length() < 2
                || normalized.length() > 20) {
            throw new IllegalArgumentException(
                    "닉네임은 2~20자로 입력해주세요."
            );
        }

        this.nickname = normalized;
    }
}

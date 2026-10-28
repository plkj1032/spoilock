
package com.spoilock.backend.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "code_expires_at", nullable = false)
    private LocalDateTime codeExpiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "proof_expires_at")
    private LocalDateTime proofExpiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "send_count", nullable = false)
    private int sendCount;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    public static EmailVerification create(
            String email,
            String codeHash,
            LocalDateTime now
    ) {
        EmailVerification record = new EmailVerification();

        record.email = email;
        record.codeHash = codeHash;
        record.codeExpiresAt = now.plusMinutes(10);
        record.lastSentAt = now;

        record.sendCount = 1;
        record.attemptCount = 0;

        return record;
    }

    public boolean canResend(LocalDateTime now) {
        // 최소 60초 간격
        if (lastSentAt.plusSeconds(60).isAfter(now)) {
            return false;
        }

        // 단순 남용 방지: 마지막 발송 이후
        // 24시간 안에는 최대 5회
        if (lastSentAt.plusHours(24).isAfter(now)
                && sendCount >= 5) {
            return false;
        }

        return true;
    }

    public void renew(
            String newCodeHash,
            LocalDateTime now
    ) {
        boolean resetCount =
                !lastSentAt.plusHours(24).isAfter(now);

        sendCount = resetCount ? 1 : sendCount + 1;

        codeHash = newCodeHash;
        codeExpiresAt = now.plusMinutes(10);
        lastSentAt = now;
        attemptCount = 0;

        // 기존 인증 결과 무효화
        verifiedAt = null;
        verificationTokenHash = null;
        proofExpiresAt = null;
        consumedAt = null;
    }

    public void recordFailure() {
        attemptCount++;
    }

    public void completeVerification(
            String proofHash,
            LocalDateTime now
    ) {
        verifiedAt = now;
        verificationTokenHash = proofHash;
        proofExpiresAt = now.plusMinutes(30);
        consumedAt = null;
    }

    public void consume(LocalDateTime now) {
        consumedAt = now;
    }
}

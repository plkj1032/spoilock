
package com.spoilock.backend.user.service;

import com.spoilock.backend.user.entity.EmailVerification;
import com.spoilock.backend.user.repository.EmailVerificationRepository;
import com.spoilock.backend.user.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository repository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.email-from}")
    private String senderEmail;

    @Value("${app.auth.email-hmac-key}")
    private String hmacSecret;

    @PostConstruct
    void validateSecret() {
        if (hmacSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "EMAIL_CODE_HMAC_KEY는 최소 32바이트가 필요합니다."
            );
        }
    }

    // ==================================================
    // 1. 이메일 주소 정규화
    // ==================================================
    public String normalizeEmail(String email) {
        if (email == null) {
            throw badRequest("이메일을 확인해 주세요.");
        }

        String normalized = email
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalized.length() > 255
                || !normalized.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {
            throw badRequest("이메일 형식이 올바르지 않습니다.");
        }

        return normalized;
    }

    // ==================================================
    // 2. 인증번호 발송
    // ==================================================
    @Transactional
    public void sendCode(String inputEmail) {
        String email = normalizeEmail(inputEmail);

        // 가입 여부 노출을 줄이기 위해
        // 이미 가입된 이메일에도 같은 성공 응답 사용
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "회원가입을 완료할 수 없습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        EmailVerification record = repository
                .findByEmail(email)
                .orElse(null);

        if (record != null && !record.canResend(now)) {
            // 발송 제한 여부도 공개 응답에서 구분하지 않음
            return;
        }

        String code = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        String codeHash = hmac(email + ":" + code);

        if (record == null) {
            record = EmailVerification.create(
                    email,
                    codeHash,
                    now
            );
        } else {
            record.renew(codeHash, now);
        }

        repository.saveAndFlush(record);

        // 인증번호가 로그나 API 응답에 노출되지 않게 한다.
        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom(senderEmail);
        mail.setTo(email);
        mail.setSubject("[SPOILOCK] 이메일 인증번호");

        mail.setText(
                "SPOILOCK 이메일 인증번호는 다음과 같습니다.\n\n"
                        + code
                        + "\n\n유효기간: 10분\n"
                        + "본인이 요청하지 않았다면 무시해 주세요."
        );

        mailSender.send(mail);
    }

    // ==================================================
    // 3. 인증번호 검증
    // 잘못된 입력 횟수를 DB에 남기기 위해
    // ResponseStatusException 발생 시에도 커밋
    // ==================================================
    @Transactional(
            noRollbackFor = ResponseStatusException.class
    )
    public VerifyResult verifyCode(
            String inputEmail,
            String inputCode
    ) {
        String email = normalizeEmail(inputEmail);

        EmailVerification record = repository
                .findByEmail(email)
                .orElseThrow(() ->
                        badRequest("인증번호가 올바르지 않거나 만료되었습니다.")
                );

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (record.getVerifiedAt() != null) {
            throw badRequest("이미 확인한 인증번호입니다.");
        }

        if (!record.getCodeExpiresAt().isAfter(now)) {
            throw badRequest("인증번호가 만료되었습니다.");
        }

        if (record.getAttemptCount() >= 5) {
            throw badRequest("인증 시도 횟수를 초과했습니다.");
        }

        String submittedHash = hmac(
                email + ":" + inputCode
        );

        if (!secureEquals(
                record.getCodeHash(),
                submittedHash
        )) {
            record.recordFailure();

            throw badRequest("인증번호가 올바르지 않습니다.");
        }

        // 인증 성공: 가입 시 제출할 일회성 증명 토큰
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String proof = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        record.completeVerification(
                sha256(proof),
                now
        );

        return new VerifyResult(proof, 1800);
    }

    // ==================================================
    // 4. 회원가입에서 증명 토큰 사용
    // AuthService.signUp()의 트랜잭션 안에서 호출
    // ==================================================
    @Transactional
    public void consumeSignupProof(
            String inputEmail,
            String signupProof
    ) {
        String email = normalizeEmail(inputEmail);

        if (signupProof == null || signupProof.isBlank()) {
            throw badRequest("이메일 인증이 필요합니다.");
        }

        EmailVerification record = repository
                .findByEmail(email)
                .orElseThrow(() ->
                        badRequest("이메일 인증이 필요합니다.")
                );

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (record.getVerifiedAt() == null
                || record.getConsumedAt() != null
                || record.getProofExpiresAt() == null
                || !record.getProofExpiresAt().isAfter(now)) {

            throw badRequest(
                    "이메일 인증이 만료되었습니다. 다시 인증해 주세요."
            );
        }

        if (!secureEquals(
                record.getVerificationTokenHash(),
                sha256(signupProof)
        )) {
            throw badRequest("이메일 인증 정보가 일치하지 않습니다.");
        }

        record.consume(now);
    }

    // ==================================================
    // 5. HMAC-SHA256
    // ==================================================
    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            return HexFormat.of().formatHex(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "인증번호 해시 생성 실패",
                    e
            );
        }
    }

    // ==================================================
    // 6. SHA-256: 일회성 증명 토큰 해시
    // ==================================================
    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "증명 토큰 해시 생성 실패",
                    e
            );
        }
    }

    private boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.US_ASCII),
                b.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    public record VerifyResult(
            String signupProof,
            int expiresInSeconds
    ) {}
}

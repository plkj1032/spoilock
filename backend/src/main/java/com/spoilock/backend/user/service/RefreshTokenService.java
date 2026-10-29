package com.spoilock.backend.user.service;

import com.spoilock.backend.user.entity.RefreshSession;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.event.LoginSessionChangedEvent;
import com.spoilock.backend.user.repository.RefreshSessionRepository;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshSessionRepository repository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.jwt.refresh-days:7}")
    private long refreshDays;

    // ==========================================
    // 단일 기기 로그인
    // 기존 로그인 무효화 + 버전 증가
    // ==========================================
    @Transactional
    public User beginNewLogin(User user) {
        User lockedUser = userRepository
                .findByIdForUpdate(user.getId())
                .orElseThrow(this::unauthorized);

        if (lockedUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw unauthorized();
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        repository.revokeAll(lockedUser.getId(), now);

        lockedUser.incrementTokenVersion();

        eventPublisher.publishEvent(
                new LoginSessionChangedEvent(
                        lockedUser.getId(),
                        lockedUser.getTokenVersion()
                )
        );

        return lockedUser;
    }


    // 1. Refresh Token 생성 + DB 저장
    @Transactional
    public String issue(User user)
    {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        String tokenHash = hash(rawToken);

        RefreshSession session = RefreshSession.create(
                user,
                tokenHash,
                LocalDateTime.now(ZoneOffset.UTC)
                        .plusDays(refreshDays)
        );

        repository.save(session);

        return rawToken;
    }

    // 2. Refresh Token 재발급 (Rotation)
    @Transactional
    public RotatedToken rotate(String rawToken)
    {
        RefreshSession oldSession = requireActive(rawToken);

        User user = oldSession.getUser();

        if(user.getAccountStatus() != AccountStatus.ACTIVE)
        {
            throw unauthorized();
        }

        // 기존 Refresh Token 무효화
        oldSession.revoke(LocalDateTime.now(ZoneOffset.UTC));

        // 새로운 Refresh Token 발급
        String newToken = issue(user);

        return new RotatedToken(user, newToken);
    }

    // 3. 모든 Refresh Token 무효화
    @Transactional
    public void logoutAll(String rawToken) {
        RefreshSession session = requireActive(rawToken);

        User user = session.getUser();

        user.incrementTokenVersion();

        repository.revokeAll(
                user.getId(),
                LocalDateTime.now(ZoneOffset.UTC)
        );

        eventPublisher.publishEvent(
                new LoginSessionChangedEvent(
                        user.getId(),
                        user.getTokenVersion()
                )
        );
    }

    // 4. Refresh Token 유효성 검증
    private RefreshSession requireActive(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw unauthorized();
        }

        String tokenHash = hash(rawToken);

        // QueryDSL: 토큰 소유 사용자 조회
        Long userId = repository
                .findUserIdByTokenHash(tokenHash)
                .orElseThrow(this::unauthorized);

        // QueryDSL: 사용자 행 잠금
        User lockedUser = userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(this::unauthorized);

        if (lockedUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw unauthorized();
        }

        // QueryDSL: Refresh Session 잠금
        RefreshSession session = repository
                .findByHashForUpdate(tokenHash)
                .orElseThrow(this::unauthorized);

        if (!session.isUsable(LocalDateTime.now(ZoneOffset.UTC))) {
            throw unauthorized();
        }

        return session;
    }

    // 5. SHA-256 해시
    private String hash(String token)
    {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashed = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashed);
        }catch(NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(
                    "SHA-256 알고리즘 오류", e
            );
        }
    }

    private ResponseStatusException unauthorized()
    {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "유효하지 않은 Refresh Token입니다."
        );
    }

    public record RotatedToken(
            User user,
            String refreshToken
    ){}

}

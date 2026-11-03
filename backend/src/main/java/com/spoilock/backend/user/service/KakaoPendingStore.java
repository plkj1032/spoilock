package com.spoilock.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class KakaoPendingStore {

    private static final String PREFIX = "spoilock:kakao:pending:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public String create(String kakaoId)
    {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);

        redisTemplate.opsForValue()
                .set(key(token),kakaoId,TTL);

        return token;
    }

    public String read(String token)
    {
        String kakaoId = redisTemplate.opsForValue()
                .get(key(token));

        if(kakaoId == null)
        {
            throw expired();
        }

        return kakaoId;
    }

    // 가입 완료 시 원자적으로 한 번만 사용
    public String take(String token)
    {
        String kakaoId = redisTemplate.opsForValue()
                .getAndDelete(key(token));

        if(kakaoId == null)
        {
            throw expired();
        }

        return kakaoId;
    }

    private String key(String token)
    {
        if(token == null || !token.matches("[A-Za-z0-9_-]{43}"))
        {
            throw expired();
        }

        try{
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return PREFIX + HexFormat.of().formatHex(hash);
        }catch(NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("토큰 해시 오류", e);
        }
    }

    private ResponseStatusException expired()
    {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "카카오 가입 인증이 만료되었습니다. 다시 로그인해 주세요."
        );
    }
}

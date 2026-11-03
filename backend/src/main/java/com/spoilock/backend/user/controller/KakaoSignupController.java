
package com.spoilock.backend.user.controller;

import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.service.AuthService;
import com.spoilock.backend.user.service.KakaoLoginService;
import com.spoilock.backend.user.service.KakaoPendingStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoSignupController {

    private final KakaoPendingStore pendingStore;
    private final KakaoLoginService kakaoLoginService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.access-minutes:15}")
    private long accessMinutes;

    @Value("${app.jwt.refresh-days:7}")
    private long refreshDays;

    public record CompleteRequest(
            @Size(max = 20)
            String nickname,

            @NotNull
            @Size(max = 10)
            List<Long> acceptedTermsIds
    ) {}

    // ==========================================
    // 1. 카카오 가입 진행 상태
    // ==========================================
    @GetMapping("/pending")
    public KakaoLoginService.PendingInfo pending(
            @CookieValue(
                    value = "kakao_pending",
                    required = false
            ) String token
    ) {
        String kakaoId = pendingStore.read(token);

        return kakaoLoginService.pendingInfo(kakaoId);
    }

    // ==========================================
    // 2. 필수 약관 동의 + 가입 완료
    // ==========================================
    @PostMapping("/complete")
    public ResponseEntity<MeResponse> complete(
            @CookieValue(
                    value = "kakao_pending",
                    required = false
            ) String token,

            @Valid @RequestBody CompleteRequest request
    ) {
        // 같은 임시 토큰으로 중복 가입하지 못하도록
        // 1회만 사용. 실패 시 카카오 인증부터 재시도
        String kakaoId = pendingStore.take(token);

        AuthService.LoginResult result =
                kakaoLoginService.complete(
                        kakaoId,
                        request.nickname(),
                        request.acceptedTermsIds()
                );

        ResponseCookie access = cookie(
                "access_token",
                result.accessToken(),
                "/",
                Duration.ofMinutes(accessMinutes)
        );

        ResponseCookie refresh = cookie(
                "refresh_token",
                result.refreshToken(),
                "/api/auth",
                Duration.ofDays(refreshDays)
        );

        ResponseCookie clearPending = cookie(
                "kakao_pending",
                "",
                "/api/auth/kakao",
                Duration.ZERO
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        access.toString(),
                        refresh.toString(),
                        clearPending.toString()
                )
                .body(result.user());
    }

    private ResponseCookie cookie(
            String name,
            String value,
            String path,
            Duration maxAge
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}

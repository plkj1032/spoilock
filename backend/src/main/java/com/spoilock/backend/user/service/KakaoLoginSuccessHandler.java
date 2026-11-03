
package com.spoilock.backend.user.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class KakaoLoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final KakaoLoginService kakaoLoginService;
    private final KakaoPendingStore pendingStore;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.access-minutes:15}")
    private long accessMinutes;

    @Value("${app.jwt.refresh-days:7}")
    private long refreshDays;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User kakaoUser =
                (OAuth2User) authentication.getPrincipal();

        KakaoLoginService.OAuthResult flow =
                kakaoLoginService.login(kakaoUser);

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        // 신규 또는 미동의 회원
        if (flow.pendingKakaoId() != null) {

            String token = pendingStore.create(
                    flow.pendingKakaoId()
            );

            // 이전 로그인 쿠키가 남아 있어도 제거
            addCookie(response, cookie(
                    "access_token", "", "/", Duration.ZERO
            ));

            addCookie(response, cookie(
                    "refresh_token", "", "/api/auth",
                    Duration.ZERO
            ));

            addCookie(response, cookie(
                    "kakao_pending",
                    token,
                    "/api/auth/kakao",
                    Duration.ofMinutes(10)
            ));

            response.sendRedirect(
                    "/auth/kakao/complete"
            );

            return;
        }

        // 필수 약관 동의 완료된 기존 회원
        AuthService.LoginResult result =
                flow.loginResult();

        addCookie(response, cookie(
                "access_token",
                result.accessToken(),
                "/",
                Duration.ofMinutes(accessMinutes)
        ));

        addCookie(response, cookie(
                "refresh_token",
                result.refreshToken(),
                "/api/auth",
                Duration.ofDays(refreshDays)
        ));

        addCookie(response, cookie(
                "kakao_pending",
                "",
                "/api/auth/kakao",
                Duration.ZERO
        ));

        response.sendRedirect("/");
    }

    private ResponseCookie cookie(
            String name,
            String value,
            String path,
            Duration maxAge
    ) {
        return ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private void addCookie(
            HttpServletResponse response,
            ResponseCookie cookie
    ) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }
}

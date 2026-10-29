
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

        AuthService.LoginResult result =
                kakaoLoginService.login(kakaoUser);

        // SPOILOCK Access Token 쿠키
        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", result.accessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(accessMinutes))
                .build();

        // SPOILOCK Refresh Token 쿠키
        ResponseCookie refreshCookie = ResponseCookie
                .from("refresh_token", result.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshDays))
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        // OAuth2 state 검증에 사용한 임시 세션 정리
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        // React 페이지로 이동
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION,"/");
    }
}

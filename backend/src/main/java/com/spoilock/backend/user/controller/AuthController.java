package com.spoilock.backend.user.controller;

import com.spoilock.backend.user.dto.LoginRequest;
import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.dto.SignUpRequest;
import com.spoilock.backend.user.dto.SignUpResponse;
import com.spoilock.backend.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.access-minutes}")
    private long accessMinutes;

    @Value("${app.jwt.refresh-days:7}")
    private long refreshDays;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(
            @Valid @RequestBody SignUpRequest request
            )
    {
        return authService.signUp(request);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(
            @Valid @RequestBody LoginRequest request
            )
    {
        AuthService.LoginResult result = authService.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createAccessCookie(
                                result.accessToken()
                        ).toString(),
                        createRefreshCookie(
                                result.refreshToken()
                        ).toString()
                )
                .body(result.user());
    }

    // 로그인 사용자 정보
    @GetMapping("/me")
    public MeResponse me(
            @AuthenticationPrincipal Jwt jwt
            ){
        return authService.getMe(
                Long.parseLong(jwt.getSubject())
        );
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/refresh")
    public ResponseEntity<MeResponse> refresh(
            @CookieValue(
                    value = "refresh_token",
                    required = false
            )String refreshToken
    ){
        AuthService.LoginResult result =
                authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createAccessCookie(
                                result.accessToken()
                        ).toString(),
                        createRefreshCookie(
                                result.refreshToken()
                        ).toString()
                )
                .body(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    value = "refresh_token",
                    required = false
            ) String refreshToken
    )
    {
        authService.logout(refreshToken);

        ResponseCookie deleteAccess =
                ResponseCookie.from("access_token", "")
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        ResponseCookie deleteRefresh =
                ResponseCookie.from("refresh_token","")
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/api/auth")
                        .maxAge(0)
                        .build();

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteAccess.toString(),
                        deleteRefresh.toString()
                )
                .build();
    }

    // Access Token 쿠키
    private ResponseCookie createAccessCookie(String token)
    {
        return ResponseCookie
                .from("access_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(accessMinutes))
                .build();
    }

    // Refresh Token 쿠키
    private ResponseCookie createRefreshCookie(String token)
    {
        return ResponseCookie
                .from("refresh_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshDays))
                .build();
    }

    // 닉네임 중복 확인
    @GetMapping("/nickname/check")
    public NicknameCheckResponse checkNickname(
            @RequestParam String nickname
    )
    {
        return new NicknameCheckResponse(
                authService.isNicknameAvailable(nickname)
        );
    }

    public record NicknameCheckResponse(boolean available) {}


}

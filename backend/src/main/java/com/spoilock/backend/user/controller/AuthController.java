package com.spoilock.backend.user.controller;

import com.spoilock.backend.user.dto.LoginRequest;
import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.dto.SignUpRequest;
import com.spoilock.backend.user.dto.SignUpResponse;
import com.spoilock.backend.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtDecoder jwtDecoder;

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

    // 로그인 상태 확인 및 필요할 때만 토큰 갱신
    // 쿠키를 갱신할 수 있으므로 GET 대신 POST 사용
    @PostMapping("/session")
    public ResponseEntity<MeResponse> session(
            @CookieValue(
                    value = "access_token",
                    required = false
            ) String accessToken,

            @CookieValue(
                    value = "refresh_token",
                    required = false
            ) String refreshToken
    ) {
        // 1. Access Token이 유효하면 갱신 없이 사용자 정보 반환
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Jwt jwt = jwtDecoder.decode(accessToken);

                MeResponse user = authService.getMe(
                        Long.valueOf(jwt.getSubject())
                );

                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(user);

            } catch (BadJwtException ex) {
                // 만료되었거나 유효하지 않은 JWT이면 아래에서 복구 시도
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() != 401) {
                    throw ex;
                }
            }
        }

        // 2. Refresh Token도 없으면 정상적인 비로그인 상태
        if (refreshToken == null || refreshToken.isBlank()) {
            return anonymousSession();
        }

        // 3. 기존 Refresh Token 검증·회전 로직으로 복구
        try {
            AuthService.LoginResult result =
                    authService.refresh(refreshToken);

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(
                            HttpHeaders.SET_COOKIE,
                            createAccessCookie(result.accessToken()).toString(),
                            createRefreshCookie(result.refreshToken()).toString()
                    )
                    .body(result.user());

        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 401) {
                return anonymousSession();
            }

            // 인증 실패 외 서버 오류는 숨기지 않음
            throw ex;
        }
    }

    private ResponseEntity<MeResponse> anonymousSession() {
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
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


package com.spoilock.backend.common.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    // ==========================================
    // 1. Spring Security 설정
    // ==========================================
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        // JWT의 role 클레임을 Spring Security 권한으로 변환
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + jwt.getClaimAsString("role")
                        )
                )
        );

        http
                // CORS 설정 적용
                .cors(Customizer.withDefaults())

                // ==========================================
                // 2. CSRF 보호
                // ==========================================
                .csrf(csrf -> csrf
                        .spa()
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository
                                        .withHttpOnlyFalse()
                        )
                )

                // ==========================================
                // 3. JWT 기반 Stateless 인증
                // ==========================================
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // ==========================================
                // 4. API 접근 권한
                // ==========================================
                .authorizeHttpRequests(auth -> auth

                        // 오류 페이지 접근 허용
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // ==================================
                        // 일반 회원가입 / 로그인
                        // ==================================
                        .requestMatchers(
                                HttpMethod.POST,

                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/session",
                                "/api/auth/logout",

                                "/api/auth/email/send",
                                "/api/auth/email/verify",

                                // 신규: 카카오 가입 완료
                                "/api/auth/kakao/complete"

                        ).permitAll()

                        // ==================================
                        // 공개 GET API
                        // ==================================
                        .requestMatchers(
                                HttpMethod.GET,

                                "/api/auth/csrf",
                                "/api/auth/nickname/check",

                                "/api/terms/current",

                                "/api/dramas",
                                "/api/dramas/**",

                                // 신규: 카카오 임시 가입 상태
                                "/api/auth/kakao/pending"

                        ).permitAll()

                        // 공개 채팅방 목록
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/chat/rooms"
                        ).permitAll()

                        // 채팅 테스트 페이지
                        .requestMatchers(
                                "/chat-test.html"
                        ).permitAll()

                        // WebSocket 인증
                        .requestMatchers(
                                "/ws",
                                "/ws/**"
                        ).authenticated()

                        // 관리자 API
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // 그 외 모든 API는 인증 필요
                        .anyRequest().authenticated()
                )

                // 기본 로그인 비활성화
                .formLogin(form -> form.disable())

                // HTTP Basic 인증 비활성화
                .httpBasic(basic -> basic.disable())

                // ==========================================
                // 5. 쿠키 기반 JWT 인증
                // ==========================================
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(
                                cookieTokenResolver()
                        )
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        converter
                                )
                        )
                );

        return http.build();
    }

    // ==========================================
    // 6. JWT 쿠키 추출
    // ==========================================
    @Bean
    public BearerTokenResolver cookieTokenResolver() {

        return request -> {

            String path = request.getRequestURI();
            String method = request.getMethod();

            // ======================================
            // 공개 채팅방 목록
            // ======================================
            // 비밀방 참여 정보 조회 시에는 인증 필요
            if ("GET".equals(method)
                    && path.equals("/api/chat/rooms")
                    && !Boolean.parseBoolean(
                    request.getParameter(
                            "includeJoinedPrivate"
                    )
            )) {

                return null;
            }

            // ======================================
            // 공개 조회 API
            // ======================================
            // 만료된 Access Token이 남아 있어도
            // 공개 API는 사용할 수 있도록 처리
            if ("GET".equals(method)
                    && (
                    path.equals("/api/dramas")
                            || (
                            path.startsWith("/api/dramas/")
                                    && !path.matches("/api/dramas/[0-9]+/rating/?")
                    )
                            || path.equals(
                            "/api/auth/nickname/check"
                    )
                            || path.equals("/api/terms/current")

                            // 카카오 추가 가입 상태 조회
                            || path.equals(
                            "/api/auth/kakao/pending"
                    )
            )) {

                return null;
            }

            // ======================================
            // 공개 인증 API
            // ======================================
            // 기존 JWT 쿠키가 만료되어도
            // 로그인 및 가입 진행 가능
            if (path.equals("/api/auth/login")
                    || path.equals("/api/auth/signup")
                    || path.equals("/api/auth/csrf")
                    || path.equals("/api/auth/refresh")
                    || path.equals("/api/auth/session")
                    || path.equals("/api/auth/logout")
                    || path.equals("/api/auth/email/send")
                    || path.equals("/api/auth/email/verify")
                    || (
                    "POST".equals(method)
                            && path.equals(
                            "/api/auth/kakao/complete"
                    )
            )) {

                return null;
            }

            // ======================================
            // Access Token 쿠키 조회
            // ======================================
            Cookie[] cookies = request.getCookies();

            if (cookies == null) {
                return null;
            }

            return Arrays.stream(cookies)
                    .filter(cookie ->
                            "access_token".equals(
                                    cookie.getName()
                            )
                    )
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        };
    }

    // ==========================================
    // 7. CORS 설정
    // ==========================================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        // 로컬 React 개발 서버
        config.setAllowedOrigins(
                List.of(
                        "http://localhost:5173"
                )
        );

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of(
                        "Content-Type",
                        "X-XSRF-TOKEN"
                )
        );

        // HttpOnly 쿠키 사용 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }
}

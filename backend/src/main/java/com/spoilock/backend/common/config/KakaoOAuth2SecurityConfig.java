package com.spoilock.backend.common.config;

import com.spoilock.backend.user.service.KakaoLoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class KakaoOAuth2SecurityConfig {

    private final KakaoLoginSuccessHandler kakaoLoginSuccessHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain kakaoOAuth2SecurityFilterChain(
            HttpSecurity http
    ) throws Exception
    {
        http
                // 카카오 OAuth2 로그인 관련 경로만 분리
                .securityMatcher(
                        "/oauth2/**",
                        "/login/oauth2/**"
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // 카카오 인증 중 state 저장을 위한 임시 세션
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // 카카오 OAuth2 로그인
                .oauth2Login(oauth -> oauth
                        .successHandler(kakaoLoginSuccessHandler)
                );
        return http.build();
    }

}

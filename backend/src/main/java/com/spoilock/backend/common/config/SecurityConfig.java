package com.spoilock.backend.common.config;

import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + jwt.getClaimAsString("role")
                        )
                ));

        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .spa()
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                )

        // Spring Security 7: React SPA CSRF 보호
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        ).permitAll()

                        .requestMatchers("/chat-test.html").permitAll()
                        .requestMatchers("/ws", "/ws/**").authenticated()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(cookieTokenResolver())
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(converter)
                        )
                );

        return http.build();

    }

    @Bean
    public BearerTokenResolver cookieTokenResolver()
    {
        return request -> {
            String path = request.getRequestURI();

            // 공개 로그인 API에서는 만료된 기존
            // Access Token 쿠키 때문에 로그인이
            // 차단되지 않도록 인증 토큰 추출을 생략
            if (path.equals("/api/auth/login")
                    || path.equals("/api/auth/signup")
                    || path.equals("/api/auth/csrf")
                    || path.equals("/api/auth/refresh")
                    || path.equals("/api/auth/logout")) {
                return null;
            }

            Cookie[] cookies = request.getCookies();

            if(cookies == null)
            {
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Content-Type",
                "X-XSRF-TOKEN"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**",config);

        return source;
    }
}
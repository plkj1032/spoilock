package com.spoilock.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local")
public class LocalTestSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain localTestSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/test/chat/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/test/chat/messages",
                                "/api/test/chat/enter",
                                "/api/test/chat/leave"
                        ).permitAll()
                        .anyRequest().denyAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/test/chat/**")
                );

        return http.build();
    }
}

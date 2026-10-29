package com.spoilock.backend.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final ChatStompInterceptor chatStompInterceptor;

    @Value("${app.websocket.allowed-origins:http://localhost:5173,http://localhost:8080}")
    private String allowedOrigins;

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ){
        registration.interceptors(chatStompInterceptor);
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {

        // Spring -> Client
        registry.enableSimpleBroker(
                "/topic",
                "/queue"
        );

        // Client -> Spring
        registry.setApplicationDestinationPrefixes("/app");

        // 추후 개인별 스포 필터 / 강퇴에 사용
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin->!origin.isEmpty())
                .toArray(String[]::new);

        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins);
    }
}
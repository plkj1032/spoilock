package com.spoilock.backend.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ChatStompInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    private static final Set<String> SEND_DESTINATIONS = Set.of(
            "/app/chat.send",
            "/app/chat.enter",
            "/app/chat.leave",
            "/app/chat.presence"
    );

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            throw new AccessDeniedException("잘못된 메시지입니다.");
        }

        StompCommand command = accessor.getCommand();

        // heartbeat와 연결 정리 메시지는 그대로 처리
        if (command == null
                || command == StompCommand.DISCONNECT
                || command == StompCommand.UNSUBSCRIBE) {
            return message;
        }

        if (!(accessor.getUser() instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || authentication instanceof
                org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AccessDeniedException("JWT 인증이 필요합니다.");
        }

        try {
            // 연결할 때 사용한 JWT를 현재 기준으로 다시 검증
            jwtDecoder.decode(
                    jwtAuthentication.getToken().getTokenValue()
            );
        } catch (JwtException e) {
            throw new AccessDeniedException(
                    "인증이 만료되었거나 더 이상 유효하지 않습니다.",
                    e
            );
        }

        String destination = accessor.getDestination();

        if (command == StompCommand.CONNECT
                || command == StompCommand.STOMP) {
            return message;
        }

        if (command == StompCommand.SUBSCRIBE
                && (
                "/user/queue/chat".equals(destination)
                        || "/topic/chat.rooms".equals(destination)
        )) {
            return message;
        }

        if (command == StompCommand.SEND
                && destination != null
                && SEND_DESTINATIONS.contains(destination)) {
            return message;
        }

        throw new AccessDeniedException(
                "허용되지 않은 STOMP 요청입니다."
        );
    }
}
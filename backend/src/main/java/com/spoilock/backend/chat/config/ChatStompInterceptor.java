package com.spoilock.backend.chat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ChatStompInterceptor implements ChannelInterceptor {

    private static final Set<String> SEND_DESTINATIONS = Set.of(
            "/app/chat.send",
            "/app/chat.enter",
            "/app/chat.leave"
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

        String destination = accessor.getDestination();

        if (command == StompCommand.CONNECT
                || command == StompCommand.STOMP) {
            return message;
        }

        if (command == StompCommand.SUBSCRIBE
                && "/user/queue/chat".equals(destination)) {
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
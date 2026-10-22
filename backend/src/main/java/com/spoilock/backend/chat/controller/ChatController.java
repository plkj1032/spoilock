package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.domain.dto.ChatRoomRequest;
import com.spoilock.backend.chat.domain.dto.ChatSendRequest;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import com.spoilock.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;

    @MessageMapping("/chat.send")
    public void send(
            @Valid @Payload ChatSendRequest request,
            Principal principal
    ) {
        Long userId = getUserId(principal);

        ChatRedisEvent event = chatService.saveMessage(
                request.roomId(),
                userId,
                request.content()
        );

        chatRedisPublisher.publish(event);
    }

    @MessageMapping("/chat.enter")
    public void enter(
            @Valid @Payload ChatRoomRequest request,
            Principal principal
    ) {
        Long userId = getUserId(principal);

        chatService.enterRoom(request.roomId(), userId)
                .ifPresent(chatRedisPublisher::publish);
    }

    @MessageMapping("/chat.leave")
    public void leave(
            @Valid @Payload ChatRoomRequest request,
            Principal principal
    ) {
        Long userId = getUserId(principal);

        chatService.leaveRoom(request.roomId(), userId)
                .ifPresent(chatRedisPublisher::publish);
    }

    private Long getUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException(
                    "인증된 사용자만 이용할 수 있습니다."
            );
        }

        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "인증 사용자 ID 형식이 올바르지 않습니다.",
                    e
            );
        }
    }
}
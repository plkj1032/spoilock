package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatTestSendRequest;
import com.spoilock.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Profile("local")
public class ChatLocalTestController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.test.send")
    public void send(
            @Valid @Payload ChatTestSendRequest request
    ) {
        Long messageId = chatService.saveMessage(
                request.roomId(),
                request.userId(),
                request.content()
        );

        messagingTemplate.convertAndSend(
                "/topic/chat/" + request.roomId(),
                new ChatTestResponse(
                        messageId,
                        request.roomId(),
                        request.userId(),
                        request.content()
                )
        );
    }

    public record ChatTestResponse(
            Long messageId,
            Long roomId,
            Long userId,
            String content
    ) {
    }
}
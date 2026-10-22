package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.domain.dto.ChatTestSendRequest;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import com.spoilock.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Profile("local")
public class ChatLocalTestController {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;


    @MessageMapping("/chat.test.send")
    public void send(
            @Valid @Payload ChatTestSendRequest request
    ) {

        ChatRedisEvent event =
                chatService.saveMessage(
                        request.roomId(),
                        request.userId(),
                        request.content()
                );

        chatRedisPublisher.publish(event);
    }
}
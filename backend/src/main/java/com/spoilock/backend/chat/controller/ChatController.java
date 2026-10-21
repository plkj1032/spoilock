package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatSendRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(
            @Valid @Payload ChatSendRequest request){
        messagingTemplate.convertAndSend("/topic/chat/" + request.roomId(), request);
    }
}

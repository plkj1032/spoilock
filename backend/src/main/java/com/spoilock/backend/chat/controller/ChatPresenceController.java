package com.spoilock.backend.chat.controller;


import com.spoilock.backend.chat.domain.dto.ChatRoomRequest;
import com.spoilock.backend.chat.service.ChatPresenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatPresenceController {
    private final ChatPresenceService chatPresenceService;

    @MessageMapping("/chat.presence")
    public void presence(
            @Valid @Payload ChatRoomRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
            ){
        if(principal == null){
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        Long userId = Long.valueOf(principal.getName());

        chatPresenceService.touch(
                sessionId,
                request.roomId(),
                userId
        );
    }
}

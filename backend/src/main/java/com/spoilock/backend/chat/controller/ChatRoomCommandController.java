package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatRoomCreateRequest;
import com.spoilock.backend.chat.domain.dto.ChatRoomCreateResponse;
import com.spoilock.backend.chat.service.ChatRoomCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomCommandController {

    private final ChatRoomCommandService chatRoomCommandService;

    @PostMapping
    public ChatRoomCreateResponse createRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return chatRoomCommandService.createRoom(
                request,
                userId
        );
    }
}
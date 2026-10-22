package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatMessageHistoryResponse;
import com.spoilock.backend.chat.domain.dto.ChatMessageResponse;
import com.spoilock.backend.chat.service.ChatMessageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatMessageQueryController {

    private final ChatMessageQueryService chatMessageQueryService;

    @GetMapping("/{roomId}/messages/{messageId}")
    public ChatMessageResponse getMessage(
            @PathVariable("roomId") Long roomId,
            @PathVariable("messageId") Long messageId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return chatMessageQueryService.getMessage(
                roomId,
                messageId,
                userId
        );
    }

    @GetMapping("/{roomId}/messages")
    public ChatMessageHistoryResponse getHistory(
            @PathVariable("roomId") Long roomId,
            @RequestParam(value = "cursor", required = false) Long cursor,
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = Long.parseLong(jwt.getSubject());
        return chatMessageQueryService.getHistory(
                roomId,
                cursor,
                userId
        );
    }
}
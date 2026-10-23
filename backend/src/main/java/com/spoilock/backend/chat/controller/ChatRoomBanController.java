package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatBanResponse;
import com.spoilock.backend.chat.service.ChatRoomBanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms/{roomId}/bans")
public class ChatRoomBanController {
    private final ChatRoomBanService chatRoomBanService;

    @GetMapping
    public List<ChatBanResponse> getActiveBans(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = Long.valueOf(jwt.getSubject());

        return chatRoomBanService.getActiveBans(roomId, actorId);
    }

    @PostMapping("/{banId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseBan(
            @PathVariable("roomId") Long roomId,
            @PathVariable("banId") Long banId,
            @AuthenticationPrincipal Jwt jwt){
        Long actorId = Long.valueOf(jwt.getSubject());

        chatRoomBanService.releaseBan(roomId, banId, actorId);
    }
}

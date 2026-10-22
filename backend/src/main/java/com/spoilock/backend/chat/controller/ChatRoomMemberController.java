package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import com.spoilock.backend.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomMemberController {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;

    @PostMapping("/{roomId}/enter")
    public MembershipResponse enter(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        Optional<ChatRedisEvent> event =
                chatService.enterRoom(roomId, userId);

        event.ifPresent(chatRedisPublisher::publish);

        return new MembershipResponse(
                roomId,
                true,
                event.isPresent(),
                event.map(ChatRedisEvent::messageId).orElse(null)
        );
    }

    @PostMapping("/{roomId}/leave")
    public MembershipResponse leave(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        Optional<ChatRedisEvent> event =
                chatService.leaveRoom(roomId, userId);

        event.ifPresent(chatRedisPublisher::publish);

        return new MembershipResponse(
                roomId,
                false,
                event.isPresent(),
                event.map(ChatRedisEvent::messageId).orElse(null)
        );
    }

    public record MembershipResponse(
            Long roomId,
            boolean joined,
            boolean changed,
            Long messageId
    ) {
    }
}
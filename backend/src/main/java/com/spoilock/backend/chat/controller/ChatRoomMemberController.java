package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatKickRequest;
import com.spoilock.backend.chat.domain.dto.ChatMemberStatusResponse;
import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import com.spoilock.backend.chat.service.ChatRoomMemberQueryService;
import com.spoilock.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomMemberController {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final ChatRoomMemberQueryService chatRoomMemberQueryService;

    @GetMapping("/{roomId}/members")
    public List<ChatMemberStatusResponse> getMembers(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return chatRoomMemberQueryService.getMembers(roomId, userId);
    }

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

    @PostMapping("/{roomId}/kick")
    public ChatRedisEvent kick(
            @PathVariable("roomId") Long roomId,
            @Valid @RequestBody ChatKickRequest request,
            @AuthenticationPrincipal Jwt jwt
            ){
        Long actorId = Long.valueOf(jwt.getSubject());

        ChatRedisEvent event = chatService.kickMember(
                roomId,
                actorId,
                request.userId(),
                request.reason()
        );
        chatRedisPublisher.publish(event);

        return event;
    }

    public record MembershipResponse(
            Long roomId,
            boolean joined,
            boolean changed,
            Long messageId
    ) {
    }
}
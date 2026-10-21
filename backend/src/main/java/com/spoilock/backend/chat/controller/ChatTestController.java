package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatTestRoomRequest;
import com.spoilock.backend.chat.domain.dto.ChatTestSendRequest;
import com.spoilock.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Profile("local")
@RequestMapping("/api/test/chat")
public class ChatTestController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/messages")
    public Map<String, Long> saveMessage(
            @Valid @RequestBody ChatTestSendRequest request
            ){
        Long messageId = chatService.saveMessage(
                request.roomId(),
                request.userId(),
                request.content()
        );
        return Map.of("messageId", messageId);

    }
    @PostMapping("/enter")
    public RoomActionResponse enter(
            @Valid @RequestBody ChatTestRoomRequest request
    ) {
        return chatService.enterRoom(request.roomId(), request.userId())
                .map(id -> {
                    sendSystemMessage(id, request, "ENTER", "입장했습니다.");
                    return new RoomActionResponse(true, id);
                })
                .orElseGet(() -> new RoomActionResponse(false, null));
    }

    @PostMapping("/leave")
    public RoomActionResponse leave(
            @Valid @RequestBody ChatTestRoomRequest request
    ) {
        return chatService.leaveRoom(request.roomId(), request.userId())
                .map(id -> {
                    sendSystemMessage(id, request, "LEAVE", "퇴장했습니다.");
                    return new RoomActionResponse(true, id);
                })
                .orElseGet(() -> new RoomActionResponse(false, null));
    }

    public record RoomActionResponse(
            boolean changed,
            Long messageId
    ) {
    }
    private void sendSystemMessage(
            Long messageId,
            ChatTestRoomRequest request,
            String messageType,
            String action
    ) {
        messagingTemplate.convertAndSend(
                "/topic/chat/" + request.roomId(),
                new SystemMessageResponse(
                        messageId,
                        request.roomId(),
                        request.userId(),
                        messageType,
                        "회원 " + request.userId() + "님이 " + action
                )
        );
    }

    public record SystemMessageResponse(
            Long messageId,
            Long roomId,
            Long userId,
            String messageType,
            String content
    ) {
    }
}

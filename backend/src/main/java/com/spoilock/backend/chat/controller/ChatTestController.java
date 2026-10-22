package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.domain.dto.ChatTestRoomRequest;
import com.spoilock.backend.chat.domain.dto.ChatTestSendRequest;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import com.spoilock.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Profile("local")
@RequestMapping("/api/test/chat")
public class ChatTestController {

    private final ChatService chatService;
    private final ChatRedisPublisher chatRedisPublisher;


    @PostMapping("/messages")
    public Map<String, Long> saveMessage(
            @Valid @RequestBody ChatTestSendRequest request
    ) {

        ChatRedisEvent event =
                chatService.saveMessage(
                        request.roomId(),
                        request.userId(),
                        request.content()
                );

        chatRedisPublisher.publish(event);

        return Map.of(
                "messageId",
                event.messageId()
        );
    }


    @PostMapping("/enter")
    public RoomActionResponse enter(
            @Valid @RequestBody ChatTestRoomRequest request
    ) {

        return chatService
                .enterRoom(
                        request.roomId(),
                        request.userId()
                )
                .map(event -> {

                    chatRedisPublisher.publish(event);

                    return new RoomActionResponse(
                            true,
                            event.messageId()
                    );
                })
                .orElseGet(() ->
                        new RoomActionResponse(
                                false,
                                null
                        )
                );
    }


    @PostMapping("/leave")
    public RoomActionResponse leave(
            @Valid @RequestBody ChatTestRoomRequest request
    ) {

        return chatService
                .leaveRoom(
                        request.roomId(),
                        request.userId()
                )
                .map(event -> {

                    chatRedisPublisher.publish(event);

                    return new RoomActionResponse(
                            true,
                            event.messageId()
                    );
                })
                .orElseGet(() ->
                        new RoomActionResponse(
                                false,
                                null
                        )
                );
    }


    public record RoomActionResponse(
            boolean changed,
            Long messageId
    ) {
    }
}
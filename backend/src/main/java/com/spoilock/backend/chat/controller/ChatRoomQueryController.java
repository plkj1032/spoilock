package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatRoomListResponse;
import com.spoilock.backend.chat.service.ChatRoomQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomQueryController {

    private final ChatRoomQueryService chatRoomQueryService;

    @GetMapping
    public List<ChatRoomListResponse> getRooms() {
        return chatRoomQueryService.getActiveRooms();
    }

    @GetMapping("/{roomId}")
    public ChatRoomListResponse getRoom(
            @PathVariable("roomId") Long roomId
    ) {
        return chatRoomQueryService.getActiveRoom(roomId);
    }
}
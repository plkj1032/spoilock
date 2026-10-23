package com.spoilock.backend.chat.domain.dto;

import com.spoilock.backend.chat.domain.type.ChatRoomType;

public record ChatRoomListResponse(
        Long roomId,
        String roomName,
        String description,
        Long memberCount,
        ChatRoomType roomType,
        Long createdById,
        Long dramaId
) {
}
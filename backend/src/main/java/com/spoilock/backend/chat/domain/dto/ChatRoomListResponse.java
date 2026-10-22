package com.spoilock.backend.chat.domain.dto;

public record ChatRoomListResponse(
        Long roomId,
        String roomName,
        String description,
        Long memberCount
) {
}
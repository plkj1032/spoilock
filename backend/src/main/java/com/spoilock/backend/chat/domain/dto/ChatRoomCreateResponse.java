package com.spoilock.backend.chat.domain.dto;

public record ChatRoomCreateResponse(
        Long roomId,
        String roomName
) {
}
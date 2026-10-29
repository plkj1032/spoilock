package com.spoilock.backend.chat.domain.dto;

public record ChatInvitationRoomResponse(
        Long roomId,
        String roomName,
        String dramaTitle
) {
}
package com.spoilock.backend.chat.domain.dto;

public record ChatMemberResponse(
        Long userId,
        String nickname
) {
}

package com.spoilock.backend.chat.domain.dto;

public record ChatMemberStatusResponse(
        Long userId,
        String nickname,
        boolean online
) {
}

package com.spoilock.backend.chat.domain.dto;

import java.time.LocalDateTime;

public record ChatBanResponse(
        Long banId,
        Long userId,
        String nickname,
        String reason,
        LocalDateTime createdAt
) {
}

package com.spoilock.backend.chat.domain.dto;

import com.spoilock.backend.chat.domain.type.MessageType;

import java.time.LocalDateTime;

public record ChatRedisEvent(
        Long roomId,
        Long messageId
) {
}
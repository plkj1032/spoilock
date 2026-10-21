package com.spoilock.backend.chat.domain.dto;

import com.spoilock.backend.chat.domain.type.MessageType;

import java.time.LocalDateTime;

public record ChatRedisEvent(

        MessageType type,

        Long roomId,

        Long messageId,

        Long userId,

        String nickname,

        String content,

        LocalDateTime createdAt

) {
}
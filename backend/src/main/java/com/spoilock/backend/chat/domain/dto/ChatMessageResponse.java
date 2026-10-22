package com.spoilock.backend.chat.domain.dto;

import com.spoilock.backend.chat.domain.type.ChatVisibility;
import com.spoilock.backend.chat.domain.type.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long userId,
        String nickname,
        MessageType messageType,
        ChatVisibility visibility,
        String content,
        LocalDateTime createdAt
) {
}

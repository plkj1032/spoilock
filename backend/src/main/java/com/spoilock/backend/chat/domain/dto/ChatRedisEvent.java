package com.spoilock.backend.chat.domain.dto;

import com.spoilock.backend.chat.domain.type.MessageType;

import java.time.LocalDateTime;

public record ChatRedisEvent(
        Long roomId,
        Long messageId,
        Long kickedUserId
) {
    //기존 채팅/입장/퇴장 코드에서 그대로 사용
    public ChatRedisEvent(Long roomId, Long messageId){
        this(roomId, messageId, null);
    }
}
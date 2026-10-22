package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepositoryCustom {
    List<ChatMessage> findHistory(
            Long roomId,
            Long cursor,
            int limit
    );
}

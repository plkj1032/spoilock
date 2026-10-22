package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {
}

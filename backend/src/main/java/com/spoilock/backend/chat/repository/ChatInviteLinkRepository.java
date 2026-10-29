package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.entity.ChatInviteLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatInviteLinkRepository extends JpaRepository<ChatInviteLink, Long> {
}

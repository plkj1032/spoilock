package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.entity.ChatInviteLinkUse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatInviteLinkUseRepository extends JpaRepository<ChatInviteLinkUse, Long> {
}

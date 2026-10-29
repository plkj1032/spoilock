package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.entity.ChatRoomInvitation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ChatRoomInvitationRepository
        extends JpaRepository<ChatRoomInvitation, Long> {

}
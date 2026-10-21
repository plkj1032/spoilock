package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.entity.ChatRoomBan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomBanRepository  extends JpaRepository<ChatRoomBan, Long> {
}

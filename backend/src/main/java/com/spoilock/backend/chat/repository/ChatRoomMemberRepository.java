package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.entity.ChatRoomMember;
import com.spoilock.backend.chat.domain.entity.id.ChatRoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {
}

package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.dto.ChatMemberResponse;

import java.util.List;

public interface ChatRoomMemberRepositoryCustom {

    List<Long> findUserIdsByRoomId(Long roomId);
    List<ChatMemberResponse> findMembersByRoomId(Long roomId);

}
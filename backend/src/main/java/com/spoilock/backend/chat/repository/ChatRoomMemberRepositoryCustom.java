package com.spoilock.backend.chat.repository;

import java.util.List;

public interface ChatRoomMemberRepositoryCustom {

    List<Long> findUserIdsByRoomId(Long roomId);
}
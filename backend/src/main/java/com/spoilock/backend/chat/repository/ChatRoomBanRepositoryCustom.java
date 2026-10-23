package com.spoilock.backend.chat.repository;

import com.spoilock.backend.chat.domain.dto.ChatBanResponse;

import java.util.List;

public interface ChatRoomBanRepositoryCustom {
    boolean existsActiveBan(Long roomId, Long userId);

    List<ChatBanResponse> findActiveBansByRoomId(Long roomId);
}

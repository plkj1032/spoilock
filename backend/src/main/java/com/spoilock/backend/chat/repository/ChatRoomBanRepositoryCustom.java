package com.spoilock.backend.chat.repository;

public interface ChatRoomBanRepositoryCustom {
    boolean existsActiveBan(Long roomId, Long userId);
}

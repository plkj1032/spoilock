package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.RefreshSession;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshSessionRepositoryCustom {

    Optional<RefreshSession> findByHashForUpdate(String hash);

    long revokeAll(Long userId, LocalDateTime now);

}

package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long>,
RefreshSessionRepositoryCustom{
}

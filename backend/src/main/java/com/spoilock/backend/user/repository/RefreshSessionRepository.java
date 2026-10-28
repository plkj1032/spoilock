
package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.RefreshSession;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshSessionRepository
        extends JpaRepository<RefreshSession, Long>,
        RefreshSessionRepositoryCustom {
}

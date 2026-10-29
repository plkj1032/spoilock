
package com.spoilock.backend.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.user.entity.RefreshSession;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.spoilock.backend.user.entity.QRefreshSession.refreshSession;

@Repository
@RequiredArgsConstructor
public class RefreshSessionRepositoryCustomImpl
        implements RefreshSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // ==========================================
    // 1. Refresh Token 해시로 사용자 ID 조회
    // ==========================================
    @Override
    public Optional<Long> findUserIdByTokenHash(
            String hash
    ) {

        Long userId = queryFactory
                .select(refreshSession.user.id)
                .from(refreshSession)
                .where(
                        refreshSession.tokenHash.eq(hash)
                )
                .fetchOne();

        return Optional.ofNullable(userId);
    }

    // ==========================================
    // 2. Refresh Session 조회 + 비관적 쓰기 잠금
    // ==========================================
    @Override
    public Optional<RefreshSession> findByHashForUpdate(
            String hash
    ) {

        RefreshSession session = queryFactory
                .selectFrom(refreshSession)
                .where(
                        refreshSession.tokenHash.eq(hash)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        return Optional.ofNullable(session);
    }

    // ==========================================
    // 3. 사용자의 활성 Refresh Session 전체 폐기
    // ==========================================
    @Override
    public long revokeAll(
            Long userId,
            LocalDateTime now
    ) {

        return queryFactory
                .update(refreshSession)
                .set(
                        refreshSession.revokedAt,
                        now
                )
                .where(
                        refreshSession.user.id.eq(userId),
                        refreshSession.revokedAt.isNull()
                )
                .execute();
    }
}

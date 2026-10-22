package com.spoilock.backend.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.user.entity.RefreshSession;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.spoilock.backend.user.entity.QRefreshSession.refreshSession;

@Repository
@RequiredArgsConstructor
public class RefreshSessionRepositoryCustomImpl
    implements RefreshSessionRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    //Refresh Token 해시 조회 + 비관적 쓰기 잠금
    @Override
    public Optional<RefreshSession> findByHashForUpdate(
            String hash
    )
    {
        RefreshSession session = queryFactory
                .selectFrom(refreshSession)
                .where(
                        refreshSession.tokenHash.eq(hash)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        return Optional.ofNullable(session);
    }

    // 사용자의 활성 Refresh Session 전체 폐기
    @Override
    public long revokeAll(
            Long userId,
            LocalDateTime now
    )
    {
        return queryFactory
                .update(refreshSession)
                .set(refreshSession.revokedAt, now)
                .where(
                        refreshSession.user.id.eq(userId),
                        refreshSession.revokedAt.isNull()
                )
                .execute();
    }
}

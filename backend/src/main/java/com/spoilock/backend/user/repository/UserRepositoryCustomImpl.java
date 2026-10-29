
package com.spoilock.backend.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.spoilock.backend.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl
        implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Optional<User> findByIdForUpdate(Long userId) {

        User result = queryFactory
                .selectFrom(user)
                .where(user.id.eq(userId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        if (result == null) {
            return Optional.empty();
        }

        // 잠금을 획득한 뒤 DB의 최신 상태로 갱신
        // 이미 관리 중인 User가 오래된 값을 가질 수 있기 때문
        entityManager.refresh(
                result,
                LockModeType.PESSIMISTIC_WRITE
        );

        return Optional.of(result);
    }
}

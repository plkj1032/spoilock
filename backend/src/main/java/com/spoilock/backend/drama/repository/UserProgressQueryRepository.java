package com.spoilock.backend.drama.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.UserProgress;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.user.entity.User;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.spoilock.backend.drama.domain.entity.QDrama.drama;
import static com.spoilock.backend.drama.domain.entity.QEpisode.episode;
import static com.spoilock.backend.drama.domain.entity.QUserProgress.userProgress;
import static com.spoilock.backend.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class UserProgressQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<User> findLockedUser(Long userId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(user)
                        .where(user.id.eq(userId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    public Optional<Drama> findPublishedDrama(Long dramaId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(drama)
                        .where(
                                drama.id.eq(dramaId),
                                drama.status.eq(DramaStatus.PUBLISHED)
                        )
                        .fetchOne()
        );
    }

    public Optional<UserProgress> findProgress(
            Long userId,
            Long dramaId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(userProgress)
                        .where(
                                userProgress.user.id.eq(userId),
                                userProgress.drama.id.eq(dramaId)
                        )
                        .fetchOne()
        );
    }

    public boolean existsEpisode(
            Long dramaId,
            Integer episodeNumber
    ) {
        return queryFactory
                .selectOne()
                .from(episode)
                .where(
                        episode.drama.id.eq(dramaId),
                        episode.episodeNumber.eq(episodeNumber)
                )
                .fetchFirst() != null;
    }
}
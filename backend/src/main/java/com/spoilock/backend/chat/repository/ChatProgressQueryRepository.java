package com.spoilock.backend.chat.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.spoilock.backend.drama.entity.QUserProgress.userProgress;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatProgressQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<Integer> findWatchedEpisodeNumber(
            Long userId,
            Long dramaId
    ) {
        Integer watchedEpisodeNumber = queryFactory
                .select(userProgress.watchedEpisodeNumber)
                .from(userProgress)
                .where(
                        userProgress.user.id.eq(userId),
                        userProgress.drama.id.eq(dramaId)
                )
                .fetchOne();

        return Optional.ofNullable(watchedEpisodeNumber);
    }
}
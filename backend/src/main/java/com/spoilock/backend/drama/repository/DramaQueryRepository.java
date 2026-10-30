package com.spoilock.backend.drama.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.dto.DramaOptionResponse;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.spoilock.backend.drama.domain.entity.QDrama.drama;
import static com.spoilock.backend.drama.domain.entity.QEpisode.episode;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DramaQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<DramaOptionResponse> findOptions() {
        return queryFactory
                .select(
                        Projections.constructor(
                                DramaOptionResponse.class,
                                drama.id,
                                drama.title,
                                drama.totalEpisodeCount
                        )
                )
                .from(drama)
                .where(drama.status.eq(DramaStatus.PUBLISHED))
                .orderBy(drama.title.asc())
                .fetch();
    }
    public Optional<Drama> findPublishedDetail(Long dramaId) {
        Drama result = queryFactory
                .selectFrom(drama)
                .where(
                        drama.id.eq(dramaId),
                        drama.status.eq(DramaStatus.PUBLISHED)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
    public Optional<Drama> findLockedForEdit(Long dramaId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(drama)
                        .where(drama.id.eq(dramaId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    public List<Episode> findEpisodesForEdit(Long dramaId) {
        return queryFactory
                .selectFrom(episode)
                .where(episode.drama.id.eq(dramaId))
                .orderBy(episode.episodeNumber.asc())
                .fetch();
    }

    public boolean existsOtherTitle(String title, Long dramaId) {
        return queryFactory
                .selectOne()
                .from(drama)
                .where(
                        drama.title.equalsIgnoreCase(title),
                        drama.id.ne(dramaId)
                )
                .fetchFirst() != null;
    }
}
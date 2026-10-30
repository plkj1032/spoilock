package com.spoilock.backend.drama.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.dto.WatchEpisodeResponse;
import com.spoilock.backend.drama.domain.entity.EpisodeVideo;
import com.spoilock.backend.drama.domain.enums.DramaProvisionType;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.spoilock.backend.drama.domain.entity.QDrama.drama;
import static com.spoilock.backend.drama.domain.entity.QEpisode.episode;
import static com.spoilock.backend.drama.domain.entity.QEpisodeVideo.episodeVideo;

@Repository
@RequiredArgsConstructor
public class DramaWatchQueryRepository {

    private final JPAQueryFactory queryFactory;

    public boolean isWatchableDrama(Long dramaId) {
        return queryFactory
                .selectOne()
                .from(drama)
                .where(
                        drama.id.eq(dramaId),
                        drama.status.eq(DramaStatus.PUBLISHED),
                        drama.provisionType.eq(DramaProvisionType.STREAMING)
                )
                .fetchFirst() != null;
    }

    public List<WatchEpisodeResponse> findEpisodes(Long dramaId) {
        return queryFactory
                .select(Projections.constructor(
                        WatchEpisodeResponse.class,
                        episode.id,
                        episode.episodeNumber,
                        episode.title,
                        episodeVideo.id.isNotNull()
                ))
                .from(episode)
                .join(episode.drama, drama)
                .leftJoin(episodeVideo)
                .on(episodeVideo.episode.id.eq(episode.id))
                .where(
                        drama.id.eq(dramaId),
                        drama.status.eq(DramaStatus.PUBLISHED),
                        drama.provisionType.eq(DramaProvisionType.STREAMING)
                )
                .orderBy(episode.episodeNumber.asc())
                .fetch();
    }

    public Optional<EpisodeVideo> findPlayableVideo(
            Long dramaId,
            Long episodeId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(episodeVideo)
                        .join(episodeVideo.episode, episode).fetchJoin()
                        .join(episode.drama, drama).fetchJoin()
                        .where(
                                episode.id.eq(episodeId),
                                drama.id.eq(dramaId),
                                drama.status.eq(DramaStatus.PUBLISHED),
                                drama.provisionType.eq(
                                        DramaProvisionType.STREAMING
                                )
                        )
                        .fetchOne()
        );
    }
}
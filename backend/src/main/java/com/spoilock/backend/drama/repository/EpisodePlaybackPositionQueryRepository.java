package com.spoilock.backend.drama.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.entity.EpisodePlaybackPosition;
import com.spoilock.backend.drama.domain.enums.DramaProvisionType;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.spoilock.backend.drama.domain.entity.QEpisodePlaybackPosition.episodePlaybackPosition;
import static com.spoilock.backend.drama.domain.entity.QDrama.drama;
import static com.spoilock.backend.drama.domain.entity.QEpisode.episode;
import static com.spoilock.backend.drama.domain.entity.QEpisodeVideo.episodeVideo;

@Repository
@RequiredArgsConstructor
public class EpisodePlaybackPositionQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Optional<EpisodePlaybackPosition> findPosition(
            Long userId,
            Long episodeId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(episodePlaybackPosition)
                        .where(
                                episodePlaybackPosition.user.id.eq(userId),
                                episodePlaybackPosition.episode.id.eq(episodeId)
                        )
                        .fetchOne()
        );
    }
    public Optional<EpisodePlaybackPosition> findLatestPosition(
            Long userId,
            Long dramaId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(episodePlaybackPosition)
                        .join(episodePlaybackPosition.episode, episode)
                        .fetchJoin()
                        .join(episode.drama, drama)
                        .join(episodeVideo)
                        .on(episodeVideo.episode.id.eq(episode.id))
                        .where(
                                episodePlaybackPosition.user.id.eq(userId),
                                drama.id.eq(dramaId),
                                drama.status.eq(DramaStatus.PUBLISHED),
                                drama.provisionType.eq(
                                        DramaProvisionType.STREAMING
                                )
                        )
                        .orderBy(
                                episodePlaybackPosition.updatedAt.desc(),
                                episodePlaybackPosition.id.desc()
                        )
                        .fetchFirst()
        );
    }
}

package com.spoilock.backend.spoiler.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.spoiler.entity.QSpoilerEvent;
import com.spoilock.backend.spoiler.entity.SpoilerEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpoilerEventQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<SpoilerEvent> findByEpisodeId(Long episodeId){
        QSpoilerEvent event = QSpoilerEvent.spoilerEvent;

        return queryFactory
                .selectFrom(event)
                .where(event.episode.id.eq(episodeId))
                .orderBy(event.id.desc())
                .fetch();
    }

}

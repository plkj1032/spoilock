
package com.spoilock.backend.drama.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.entity.QEpisode;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class EpisodeRepositoryCustomImpl
        implements EpisodeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long deleteDraftEpisodes(Long dramaId) {

        QEpisode episode = QEpisode.episode;

        // 대기 중인 변경사항을 DB에 반영
        entityManager.flush();

        long deletedCount = new JPAQueryFactory(entityManager)
                .delete(episode)
                .where(episode.drama.id.eq(dramaId))
                .execute();

        // Bulk 연산 후 영속성 컨텍스트 초기화
        entityManager.clear();

        return deletedCount;
    }
}

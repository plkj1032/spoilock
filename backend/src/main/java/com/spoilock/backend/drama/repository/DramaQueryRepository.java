package com.spoilock.backend.drama.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.dto.DramaOptionResponse;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.spoilock.backend.drama.domain.entity.QDrama.drama;

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
}
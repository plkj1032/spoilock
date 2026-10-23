package com.spoilock.backend.drama.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.dto.DramaOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spoilock.backend.drama.entity.QDrama.drama;

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
                .orderBy(drama.title.asc())
                .fetch();
    }
}
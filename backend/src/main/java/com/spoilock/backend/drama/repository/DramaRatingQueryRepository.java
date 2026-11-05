package com.spoilock.backend.drama.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.entity.DramaRating;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static com.spoilock.backend.drama.domain.entity.QDramaRating.dramaRating;

@Repository
@RequiredArgsConstructor
public class DramaRatingQueryRepository {

    private final JPAQueryFactory queryFactory;

    public record Summary(
            BigDecimal averageRating,
            long ratingCount
    ){
    }
    public Optional<DramaRating> findMyRating(
            Long userId,
            Long dramaId
    ){
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(dramaRating)
                        .where(
                                dramaRating.user.id.eq(userId),
                                dramaRating.drama.id.eq(dramaId)
                        )
                        .fetchOne()
        );
    }
    public Summary summarize(Long dramaId){
        Tuple result = queryFactory
                .select(
                        dramaRating.id.count(),
                        dramaRating.score.avg()
                )
                .from(dramaRating)
                .where(dramaRating.drama.id.eq(dramaId))
                .fetchOne();

        if (result == null) {
            return new Summary(null,0);
        }

        Long count = result.get(dramaRating.id.count());
        Double averageScore = result.get(dramaRating.score.avg());

        if (count == null || count == 0 || averageScore == null) {
            return new Summary(null,0);
        }

        //저장 점수의 평균을 실제 별점으로 변환하고 소수 첫째자리 표시
        BigDecimal averageRating = BigDecimal
                .valueOf(averageScore)
                .divide(
                        BigDecimal.valueOf(2),
                        1,
                        RoundingMode.HALF_UP
                );
        return new Summary(averageRating,count);
    }
}

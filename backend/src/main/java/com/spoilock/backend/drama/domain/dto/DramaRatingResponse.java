package com.spoilock.backend.drama.domain.dto;

import java.math.BigDecimal;

public record DramaRatingResponse(
        Long dramaId,

        //화면 표시 점수: 0.0 ~ 5.0, 평가가 없으면 null
        BigDecimal averageRating,

        long ratingCount,

        //내 저장 점수:0~10, 미평가 null
        Integer myScore,

        //로그인+시청진도 1화 이상
        boolean canRate
) {
}

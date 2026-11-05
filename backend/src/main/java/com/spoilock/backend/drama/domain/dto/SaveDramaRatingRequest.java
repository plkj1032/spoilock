package com.spoilock.backend.drama.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveDramaRatingRequest(
        @NotNull(message = "별점을 선택해 주세요.")
        @Min(value = 0, message = "별점은 0점 이상이어야 합니다.")
        @Max(value = 10, message = "별점을 5점 이하여야합니다.")
        Integer score
) {
}

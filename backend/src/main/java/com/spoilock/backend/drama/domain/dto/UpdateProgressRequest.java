package com.spoilock.backend.drama.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProgressRequest(
        @NotNull(message = "시청 회차를 입력해주세요.")
        @Min(value = 0, message = "시청 회차는 0 이상이어야 합니다.")
        Integer watchedEpisodeNumber
) {
}

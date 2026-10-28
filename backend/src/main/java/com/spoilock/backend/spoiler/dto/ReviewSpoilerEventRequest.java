package com.spoilock.backend.spoiler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 사건 검수.
 * VERIFIED(승인)는 firstRevealEpisode(최초 공개 회차)가 필수이며 사건이 나온 회차 이하여야 한다.
 * REJECTED(반려)된 사건은 스포일러 판정 후보에서 빠진다.
 */
public record ReviewSpoilerEventRequest(
        @NotBlank
        @Pattern(regexp = "PENDING|VERIFIED|REJECTED")
        String verificationStatus,

        @Min(1)
        Integer firstRevealEpisode
) {
}

package com.spoilock.backend.drama.domain.dto;

import jakarta.validation.constraints.NotNull;

public record SavePlaybackPositionRequest(
        @NotNull Double positionSeconds,
        @NotNull Double durationSeconds
) {
}

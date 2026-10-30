package com.spoilock.backend.drama.domain.dto;

import java.time.LocalDateTime;

public record PlaybackPositionResponse(
        Long episodeId,
        boolean saved,
        double positionSeconds,
        Double durationSeconds,
        LocalDateTime updatedAt
) {
}
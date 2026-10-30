package com.spoilock.backend.drama.domain.dto;

import java.time.Instant;

public record EpisodePlaybackResponse(
        Long dramaId,
        Long episodeId,
        Integer episodeNumber,
        String title,
        String playbackUrl,
        Instant expiresAt
) {
}
package com.spoilock.backend.drama.domain.dto;

public record WatchEpisodeResponse(
        Long episodeId,
        Integer episodeNumber,
        String title,
        boolean playable
) {
}
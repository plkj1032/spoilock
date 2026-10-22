package com.spoilock.backend.drama.dto;

import com.spoilock.backend.drama.entity.Episode;

import java.time.LocalDateTime;

public record EpisodeResponse (
        Long id,
        Integer episodeNumber,
        String title,
        LocalDateTime releasedAt
){
    public static EpisodeResponse from(Episode episode)
    {
        return new EpisodeResponse(
                episode.getId(),
                episode.getEpisodeNumber(),
                episode.getTitle(),
                episode.getReleasedAt()
        );
    }
}

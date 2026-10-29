package com.spoilock.backend.drama.domain.dto;

public record ProgressResponse(
        Long dramaId,
        Integer watchedEpisodeNumber,
        Integer totalEpisodeCount
) {}

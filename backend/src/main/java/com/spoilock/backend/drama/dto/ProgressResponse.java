package com.spoilock.backend.drama.dto;

public record ProgressResponse(
        Long dramaId,
        Integer watchedEpisodeNumber,
        Integer totalEpisodeCount
) {}

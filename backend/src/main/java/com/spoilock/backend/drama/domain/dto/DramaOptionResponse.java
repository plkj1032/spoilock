package com.spoilock.backend.drama.domain.dto;

public record DramaOptionResponse(
        Long dramaId,
        String title,
        Integer totalEpisodeCount
) {
}
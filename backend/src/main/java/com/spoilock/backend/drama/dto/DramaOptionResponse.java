package com.spoilock.backend.drama.dto;

public record DramaOptionResponse(
        Long dramaId,
        String title,
        Integer totalEpisodeCount
) {
}
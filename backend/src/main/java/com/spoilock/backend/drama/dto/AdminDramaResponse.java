package com.spoilock.backend.drama.dto;

import com.spoilock.backend.drama.entity.Drama;

public record AdminDramaResponse(
        Long id,
        String title,
        Integer totalEpisodeCount,
        String status
) {
    public static AdminDramaResponse from(Drama drama){
        return new AdminDramaResponse(
                drama.getId(),
                drama.getTitle(),
                drama.getTotalEpisodeCount(),
                drama.getStatus().name()
        );
    }
}

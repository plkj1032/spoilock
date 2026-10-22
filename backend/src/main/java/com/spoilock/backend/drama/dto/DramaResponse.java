package com.spoilock.backend.drama.dto;

import com.spoilock.backend.drama.entity.Drama;

public record DramaResponse (
        Long id,
        String title,
        String synopsis,
        String posterUrl,
        Integer totalEpisodeCount
){
    public static DramaResponse from(Drama drama)
    {
        return new DramaResponse(
                drama.getId(),
                drama.getTitle(),
                drama.getSynopsis(),
                drama.getPosterUrl(),
                drama.getTotalEpisodeCount()
        );
    }
}

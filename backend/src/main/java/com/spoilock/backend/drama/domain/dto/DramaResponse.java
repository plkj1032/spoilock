package com.spoilock.backend.drama.domain.dto;

import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.enums.DramaProvisionType;

public record DramaResponse (
        Long id,
        String title,
        String synopsis,
        String posterUrl,
        Integer totalEpisodeCount,
        DramaProvisionType provisionType
){
    public static DramaResponse from(Drama drama)
    {
        return new DramaResponse(
                drama.getId(),
                drama.getTitle(),
                drama.getSynopsis(),
                drama.getPosterUrl(),
                drama.getTotalEpisodeCount(),
                drama.getProvisionType()
        );
    }
}

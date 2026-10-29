package com.spoilock.backend.drama.domain.dto;

import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.enums.DramaGenre;

import java.time.LocalDate;
import java.util.List;

public record AdminDramaDetailResponse(
        Long id,
        String title,
        String synopsis,
        String posterUrl,
        Integer totalEpisodeCount,
        String status,
        List<EpisodeResponse> episodes,
        List<DramaGenre> genres,
        LocalDate premiereDate,
        String directors,
        String writers,
        String castNames,
        String officialHomepageUrl
) {
    public static AdminDramaDetailResponse from(
            Drama drama,
            List<Episode> episodes
    )
    {
        return new AdminDramaDetailResponse(
                drama.getId(),
                drama.getTitle(),
                drama.getSynopsis(),
                drama.getPosterUrl(),
                drama.getTotalEpisodeCount(),
                drama.getStatus().name(),
                episodes.stream()
                        .map(EpisodeResponse::from)
                        .toList(),
                drama.getGenres().stream()
                        .sorted()
                        .toList(),
                drama.getPremiereDate(),
                drama.getDirectors(),
                drama.getWriters(),
                drama.getCastNames(),
                drama.getOfficialHomepageUrl()
        );
    }
}

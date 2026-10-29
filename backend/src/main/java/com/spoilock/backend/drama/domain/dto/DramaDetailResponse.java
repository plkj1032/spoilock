package com.spoilock.backend.drama.domain.dto;

import com.spoilock.backend.drama.domain.entity.Drama;

import java.time.LocalDate;
import java.util.List;

public record DramaDetailResponse (
        Long id,
        String title,
        String synopsis,
        String posterUrl,
        Integer totalEpisodeCount,
        List<GenreItem> genres,
        LocalDate premiereDate,
        String directors,
        String writers,
        String castNames,
        String officialHomepageUrl
){
    public record GenreItem(
            String code,
            String label
    ){
    }
    public static DramaDetailResponse from(Drama drama){
        return new DramaDetailResponse(
                drama.getId(),
                drama.getTitle(),
                drama.getSynopsis(),
                drama.getPosterUrl(),
                drama.getTotalEpisodeCount(),
                drama.getGenres().stream().sorted().map(genre -> new GenreItem(
                        genre.name(), genre.getLabel()
                )).toList(),
                drama.getPremiereDate(),
                drama.getDirectors(),
                drama.getWriters(),
                drama.getCastNames(),
                drama.getOfficialHomepageUrl()
        );
    }
}

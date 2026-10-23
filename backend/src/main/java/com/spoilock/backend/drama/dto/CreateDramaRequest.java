package com.spoilock.backend.drama.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateDramaRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 5000)
        String synopsis,

        @Size(max = 1000)
        String posterUrl,

        @Min(1)
        @Max(1000)
        Integer totalEpisodeCount,

        @NotEmpty
        List<@Valid EpisodeItem> episodes
) {
    public record EpisodeItem(
            @NotNull @Min(1) Integer episodeNumber,
            @NotBlank @Size(max = 200) String title
    ){}
}

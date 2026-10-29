package com.spoilock.backend.drama.domain.dto;

import com.spoilock.backend.drama.domain.enums.DramaGenre;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CreateDramaRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 5000)
        String synopsis,

        @Size(max = 1000)
        String posterUrl,

        @NotNull
        @Min(1)
        @Max(1000)
        Integer totalEpisodeCount,

        @NotEmpty
        List<@NotNull @Valid EpisodeItem> episodes,

        Set<@NotNull DramaGenre> genres,

        LocalDate premiereDate,

        @Size(max = 500)
        String directors,

        @Size(max = 500)
        String writers,

        @Size(max = 2000)
        String castNames,

        @Size(max = 1000)
        @Pattern(
                regexp = "^https?://[^\\s]+$",
                message = "공식 홈페이지 주소는 http:// 또는 https://로 시작해야 합니다."
        )
                String officialHomepageUrl
) {
    public record EpisodeItem(
            @NotNull
            @Min(1)
            Integer episodeNumber,

            @NotBlank
            @Size(max = 200)
            String title
    ) {
    }
}
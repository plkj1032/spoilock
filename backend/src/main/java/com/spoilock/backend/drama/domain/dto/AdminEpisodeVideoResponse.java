
package com.spoilock.backend.drama.domain.dto;

import com.spoilock.backend.drama.domain.entity.EpisodeVideo;

import java.time.LocalDateTime;

public record AdminEpisodeVideoResponse(
        Long episodeId,
        boolean registered,
        String filename,
        Long sizeBytes,
        LocalDateTime uploadedAt
) {

    public static AdminEpisodeVideoResponse empty(
            Long episodeId
    ) {
        return new AdminEpisodeVideoResponse(
                episodeId,
                false,
                null,
                null,
                null
        );
    }

    public static AdminEpisodeVideoResponse from(
            EpisodeVideo video
    ) {
        return new AdminEpisodeVideoResponse(
                video.getEpisode().getId(),
                true,
                video.getOriginalFilename(),
                video.getSizeBytes(),
                video.getUploadedAt()
        );
    }
}

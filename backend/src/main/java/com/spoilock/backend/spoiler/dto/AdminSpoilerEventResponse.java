package com.spoilock.backend.spoiler.dto;

import com.spoilock.backend.spoiler.entity.SpoilerEvent;

public record AdminSpoilerEventResponse(
        Long id,
        Long episodeId,
        String eventSummary,
        String sourceType,
        boolean verified,
        String indexStatus
) {
    public static AdminSpoilerEventResponse from(
            SpoilerEvent event
    )
    {
        return new AdminSpoilerEventResponse(
                event.getId(),
                event.getEpisode().getId(),
                event.getEventSummary(),
                event.getSourceType().name(),
                event.isVerified(),
                event.getIndexStatus().name()
        );
    }
}

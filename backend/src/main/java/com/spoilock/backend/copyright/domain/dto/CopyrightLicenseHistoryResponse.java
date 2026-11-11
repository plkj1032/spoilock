package com.spoilock.backend.copyright.domain.dto;

import com.spoilock.backend.copyright.domain.entity.CopyrightLicenseHistory;

import java.time.LocalDateTime;

public record CopyrightLicenseHistoryResponse(
        Long id,
        String action,
        String actorNickname,
        String beforeSnapshot,
        String afterSnapshot,
        LocalDateTime createdAt
) {
    public static CopyrightLicenseHistoryResponse from(
            CopyrightLicenseHistory history
    ) {
        return new CopyrightLicenseHistoryResponse(
                history.getId(),
                history.getAction(),
                history.getActorNickname(),
                history.getBeforeSnapshot(),
                history.getAfterSnapshot(),
                history.getCreatedAt()
        );
    }
}
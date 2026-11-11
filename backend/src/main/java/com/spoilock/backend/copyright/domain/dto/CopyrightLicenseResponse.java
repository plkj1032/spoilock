package com.spoilock.backend.copyright.domain.dto;

import com.spoilock.backend.copyright.domain.entity.CopyrightLicense;
import com.spoilock.backend.copyright.domain.type.LicenseReviewStatus;
import com.spoilock.backend.copyright.domain.type.UsagePermission;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CopyrightLicenseResponse(
        Long id,

        Long dramaId,
        String dramaTitle,

        String licenseName,
        String rightsHolder,
        String contactName,
        String contactEmail,

        LicenseReviewStatus reviewStatus,

        UsagePermission videoPermission,
        UsagePermission subtitlePermission,
        UsagePermission aiPermission,

        LocalDate startsOn,
        LocalDate endsOn,

        String scopeNotes,
        String adminNotes,

        Long createdById,
        String createdByNickname,

        Long updatedById,
        String updatedByNickname,

        long version,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CopyrightLicenseResponse from(
            CopyrightLicense license
    ) {
        return new CopyrightLicenseResponse(
                license.getId(),

                license.getDrama().getId(),
                license.getDrama().getTitle(),

                license.getLicenseName(),
                license.getRightsHolder(),
                license.getContactName(),
                license.getContactEmail(),

                license.getReviewStatus(),

                license.getVideoPermission(),
                license.getSubtitlePermission(),
                license.getAiPermission(),

                license.getStartsOn(),
                license.getEndsOn(),

                license.getScopeNotes(),
                license.getAdminNotes(),

                license.getCreatedBy().getId(),
                license.getCreatedBy().getNickname(),

                license.getUpdatedBy().getId(),
                license.getUpdatedBy().getNickname(),

                license.getVersion(),

                license.getCreatedAt(),
                license.getUpdatedAt()
        );
    }
}
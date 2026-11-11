package com.spoilock.backend.copyright.domain.dto;

import com.spoilock.backend.copyright.domain.entity.CopyrightEvidence;

import java.time.LocalDateTime;

public record CopyrightEvidenceResponse(
        Long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String sha256,
        Long uploadedById,
        String uploadedByNickname,
        LocalDateTime createdAt
) {

    public static CopyrightEvidenceResponse from(
            CopyrightEvidence evidence
    ) {
        return new CopyrightEvidenceResponse(
                evidence.getId(),
                evidence.getOriginalFilename(),
                evidence.getContentType(),
                evidence.getSizeBytes(),
                evidence.getSha256(),
                evidence.getUploadedBy().getId(),
                evidence.getUploadedBy().getNickname(),
                evidence.getCreatedAt()
        );
    }
}
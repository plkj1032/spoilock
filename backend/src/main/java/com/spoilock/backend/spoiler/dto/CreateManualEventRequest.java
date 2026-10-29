package com.spoilock.backend.spoiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 수동 사건 등록. eventType / factStatus 를 비우면 Python 기본값 (MAJOR_EVENT / DIRECT_EVENT) */
public record CreateManualEventRequest(
        @NotBlank
        @Size(max = 500)
        String eventSummary,

        String eventType,

        String factStatus
) {
}

package com.spoilock.backend.spoiler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateManualEventRequest(
        @NotBlank
        @Size(max = 2000)
        String eventSummary
) {
}

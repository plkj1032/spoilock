package com.spoilock.backend.spoiler.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewSpoilerEventRequest(
        @NotNull Boolean verified
) {
}

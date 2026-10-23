package com.spoilock.backend.chat.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChatKickRequest(
        @NotNull
        @Positive
        Long userId,

        @Size(max = 500)
        String reason
) {
}

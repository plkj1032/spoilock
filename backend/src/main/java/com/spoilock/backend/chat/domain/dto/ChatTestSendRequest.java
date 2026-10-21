package com.spoilock.backend.chat.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChatTestSendRequest(
        @NotNull
        @Positive
        Long roomId,

        @NotNull
        @Positive
        Long userId,

        @NotBlank
        @Size(max = 2000)
        String content
) {
}

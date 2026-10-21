package com.spoilock.backend.chat.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(
        @NotNull
        @Positive
        Long roomId,

        @NotBlank
        @Size(max=2000)
        String content
) {
}


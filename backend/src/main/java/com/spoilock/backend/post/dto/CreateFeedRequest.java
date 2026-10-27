package com.spoilock.backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateFeedRequest(
        @NotNull
        @Positive
        Long dramaId,

        @NotBlank
        @Size(max = 2000)
        String content
) {
}

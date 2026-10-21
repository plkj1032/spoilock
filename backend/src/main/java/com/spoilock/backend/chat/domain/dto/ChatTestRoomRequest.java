package com.spoilock.backend.chat.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatTestRoomRequest(

        @NotNull
        @Positive
        Long roomId,

        @NotNull
        @Positive
        Long userId

) {
}
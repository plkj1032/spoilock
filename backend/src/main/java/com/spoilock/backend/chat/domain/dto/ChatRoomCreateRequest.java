package com.spoilock.backend.chat.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRoomCreateRequest(

        @NotNull
        Long dramaId,

        @NotBlank
        @Size(max = 100)
        String roomName,

        String description
) {
}
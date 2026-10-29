package com.spoilock.backend.chat.domain.dto;

import com.spoilock.backend.chat.domain.type.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRoomCreateRequest(

        @NotNull
        Long dramaId,

        @NotBlank
        @Size(max = 100)
        String roomName,

        String description,

        ChatRoomType roomType
) {
}
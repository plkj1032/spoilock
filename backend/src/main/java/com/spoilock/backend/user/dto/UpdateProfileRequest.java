package com.spoilock.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(
                min = 2,
                max = 20,
                message = "닉네임은 2~20자로 입력해주세요."
        )
        String nickname
) {
    public UpdateProfileRequest {
        nickname = nickname == null ? null : nickname.trim();
    }
}
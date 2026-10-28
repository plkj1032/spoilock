
package com.spoilock.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SignUpRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @NotBlank
        @Size(min = 2, max = 20)
        String nickname,

        @NotBlank
        String signupProof,

        // 사용자가 동의한 현재 약관 ID 목록
        @NotNull
        @Size(max = 10)
        List<Long> acceptedTermsIds
) {
}

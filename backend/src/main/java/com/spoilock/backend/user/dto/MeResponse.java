package com.spoilock.backend.user.dto;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        String role
) {
}

package com.spoilock.backend.user.dto;

public record SignUpResponse (
        Long id,
        String email,
        String nickname
){
}

package com.spoilock.backend.follow.dto;

import java.time.LocalDateTime;

public record FollowUserResponse(
        Long userId,
        String nickname,
        LocalDateTime followedAt
) {
}

package com.spoilock.backend.follow.dto;

public record FollowStatsResponse(
        Long userId,
        long followerCount,
        long followingCount,
        boolean isFollowing
) {
}

package com.spoilock.backend.follow.controller;

import com.spoilock.backend.follow.dto.FollowStatsResponse;
import com.spoilock.backend.follow.dto.FollowUserResponse;
import com.spoilock.backend.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    // =========================================
    // 1. 팔로우
    // =========================================
    @PostMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowStatsResponse follow(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt
            ){
        Long loginUserId = Long.parseLong(jwt.getSubject());

        return followService.follow(
                loginUserId,
                userId
        );
    }

    // =========================================
    // 2. 언팔로우
    // =========================================
    @DeleteMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long loginUserId =
                Long.parseLong(jwt.getSubject());

        followService.unfollow(
                loginUserId,
                userId
        );
    }

    // =========================================
    // 3. 팔로우 통계 및 여부
    // =========================================
    @GetMapping("/{userId}/follow-stats")
    public FollowStatsResponse getStats(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long loginUserId =
                Long.parseLong(jwt.getSubject());

        return followService.getStats(
                userId,
                loginUserId
        );
    }

    // =========================================
    // 4. 팔로워 목록
    // =========================================
    @GetMapping("/{userId}/followers")
    public Page<FollowUserResponse> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return followService.getFollowers(
                userId,
                page,
                size
        );
    }

    // =========================================
    // 5. 팔로잉 목록
    // =========================================
    @GetMapping("/{userId}/following")
    public Page<FollowUserResponse> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return followService.getFollowing(
                userId,
                page,
                size
        );
    }

}

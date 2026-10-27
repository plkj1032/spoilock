
package com.spoilock.backend.post.controller;

import com.spoilock.backend.post.dto.CreateFeedRequest;
import com.spoilock.backend.post.dto.FeedPageResponse;
import com.spoilock.backend.post.service.FeedService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService service;

    // 전체 피드 / 팔로잉 피드
    @GetMapping
    public FeedPageResponse getFeed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return service.getFeed(
                userId,
                type,
                cursor,
                size
        );
    }

    // 새로운 피드 작성
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePostResponse createPost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateFeedRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        Long postId = service.createPost(userId, request);

        return new CreatePostResponse(
                postId,
                "PENDING"
        );
    }

    public record CreatePostResponse(
            Long postId,
            String analysisStatus
    ) {}

    @GetMapping("/users/{userId}")
    public FeedPageResponse userFeed(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getUserFeed(
                Long.valueOf(jwt.getSubject()),
                userId,
                cursor,
                size
        );
    }

    @GetMapping("/users/{userId}/preview")
    public FeedService.ProfileFeedPreview userPreview(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.getProfilePreview(
                Long.valueOf(jwt.getSubject()),
                userId
        );
    }
}

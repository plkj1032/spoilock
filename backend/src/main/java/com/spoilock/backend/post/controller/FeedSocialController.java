package com.spoilock.backend.post.controller;

import com.spoilock.backend.post.service.FeedSocialService;
import com.spoilock.backend.post.service.FeedSocialService.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed/posts")
@RequiredArgsConstructor
public class FeedSocialController {

    private final FeedSocialService service;

    public record CommentRequest(
            @NotBlank @Size(max = 1000)
            String content
    ) {}

    @GetMapping("/{postId}/social")
    public SocialSummary summary(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.summary(
                postId,
                Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{postId}/like")
    public SocialSummary like(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.like(
                postId,
                Long.valueOf(jwt.getSubject())
        );
    }

    @DeleteMapping("/{postId}/like")
    public SocialSummary unlike(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.unlike(
                postId,
                Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{postId}/comments")
    public Page<CommentResponse> comments(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page
    ) {
        return service.comments(
                postId,
                Long.valueOf(jwt.getSubject()),
                page
        );
    }

    @PostMapping("/{postId}/comments")
    public CommentResponse comment(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CommentRequest request
    ) {
        return service.comment(
                postId,
                Long.valueOf(jwt.getSubject()),
                request.content()
        );
    }
}
package com.spoilock.backend.post.controller;

import com.spoilock.backend.post.dto.UpdateFeedRequest;
import com.spoilock.backend.post.service.FeedMutationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed/posts")
@RequiredArgsConstructor
public class FeedMutationController {

    private final FeedMutationService mutationService;

    public record UpdateCommentRequest(
            @NotBlank
            @Size(max = 1000)
            String content
    ){}
    @PatchMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateFeedRequest request
            ){
        Long userId = Long.valueOf(jwt.getId());

        mutationService.updatePost(
                postId,
                userId,
                request.content(),
                request.audience()
        );
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = Long.valueOf(jwt.getId());

        mutationService.deletePost(
                postId,
                userId
        );
    }

    @PatchMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCommentRequest request
    ){
        Long userId = Long.valueOf(jwt.getId());

        mutationService.updateComment(
                postId,
                commentId,
                userId,
                request.content()
        );
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = Long.valueOf(jwt.getId());

        mutationService.deleteComment(
                postId,
                commentId,
                userId
        );
    }
}

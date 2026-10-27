package com.spoilock.backend.post.dto;

import java.time.LocalDateTime;

public record FeedItemResponse(
        Long postId,
        Long authorId,
        String authorNickname,
        Long dramaId,
        String dramaTitle,
        String content,
        boolean visible,
        String blockReason,
        String analysisStatus,
        LocalDateTime createdAt
) {
}

package com.spoilock.backend.post.dto;

import java.util.List;

public record FeedPageResponse(
        List<FeedItemResponse> items,
        String nextCursor,
        boolean hasNext
) {
}

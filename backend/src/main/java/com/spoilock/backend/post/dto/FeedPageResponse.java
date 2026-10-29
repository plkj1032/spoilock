package com.spoilock.backend.post.dto;

import java.util.List;

public record FeedPageResponse(
        List<FeedItemResponse> items,
        String nextCursor,
        boolean hasNext,
        Long totalElements
) {
    // 기존 사용자별 피드 등 3개 인자로 생성하는 코드 유지
    // 집계하지 않은 경우에는 0이 아닌 null
    public FeedPageResponse(
            List<FeedItemResponse> items,
            String nextCursor,
            boolean hasNext
    ) {
        this(items, nextCursor, hasNext, null);
    }
}
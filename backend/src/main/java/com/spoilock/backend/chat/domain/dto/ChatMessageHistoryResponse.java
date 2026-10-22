package com.spoilock.backend.chat.domain.dto;

import java.util.List;

public record ChatMessageHistoryResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasNext
) {
}

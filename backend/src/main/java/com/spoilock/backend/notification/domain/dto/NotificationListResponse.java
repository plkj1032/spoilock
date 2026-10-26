package com.spoilock.backend.notification.domain.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        Long nextCursor,
        boolean hasNext
) {
}

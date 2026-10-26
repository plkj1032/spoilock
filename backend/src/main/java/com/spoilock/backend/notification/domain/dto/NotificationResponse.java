package com.spoilock.backend.notification.domain.dto;

import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.domain.type.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        Long roomId,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getRoomId(),
                notification.getContent(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}

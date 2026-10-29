package com.spoilock.backend.notification.domain.dto;

import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.domain.type.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        Long roomId,
        Long senderUserId,
        Long invitationId,
        String content,
        boolean read,
        LocalDateTime createdAt,
        String invitationStatus
) {
    public static NotificationResponse from(
            Notification notification,
            String invitationStatus
    ) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getRoomId(),
                notification.getSenderUserId(),
                notification.getInvitationId(),
                notification.getContent(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                invitationStatus
        );
    }
}
package com.spoilock.backend.notification.service;

import com.spoilock.backend.chat.domain.type.ChatInvitationStatus;
import com.spoilock.backend.chat.repository.ChatRoomInvitationQueryRepository;
import com.spoilock.backend.notification.domain.dto.NotificationListResponse;
import com.spoilock.backend.notification.domain.dto.NotificationResponse;
import com.spoilock.backend.notification.repository.NotificationQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DISPLAY_LIMIT = 3;

    private final NotificationQueryRepository notificationQueryRepository;
    private final ChatRoomInvitationQueryRepository invitationQueryRepository;

    public NotificationListResponse getNotifications(Long userId) {
        var notifications = notificationQueryRepository
                .findLatest(userId, DISPLAY_LIMIT);

        var invitationIds = notifications.stream()
                .map(item -> item.getInvitationId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> statuses = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (var invitation : invitationQueryRepository.findForReceiver(
                userId,
                invitationIds
        )) {
            String status = invitation.getStatus().name();

            if (invitation.getStatus() == ChatInvitationStatus.PENDING
                    && !invitation.getExpiresAt().isAfter(now)) {
                status = "EXPIRED";
            }

            statuses.put(invitation.getId(), status);
        }

        var response = notifications.stream()
                .map(item -> NotificationResponse.from(
                        item,
                        item.getInvitationId() == null
                                ? null
                                : statuses.getOrDefault(
                                item.getInvitationId(),
                                "UNAVAILABLE"
                        )
                ))
                .toList();

        return new NotificationListResponse(
                response,
                null,
                false
        );
    }

    public long getUnreadCount(Long userId) {
        return notificationQueryRepository.countUnread(userId);
    }

    @Transactional
    public void markAsRead(
            Long notificationId,
            Long userId
    ) {
        long updated = notificationQueryRepository.markAsRead(
                notificationId,
                userId
        );

        if (updated > 0) {
            return;
        }

        // 이미 읽었거나 숨긴 본인 알림이면 정상 처리
        requireOwnedNotification(notificationId, userId);
    }

    @Transactional
    public void dismiss(
            Long notificationId,
            Long userId
    ) {
        long updated = notificationQueryRepository.dismiss(
                notificationId,
                userId
        );

        if (updated > 0) {
            return;
        }

        // 이미 숨긴 본인 알림이면 정상 처리
        requireOwnedNotification(notificationId, userId);
    }
    @Transactional
    public void dismissAll(Long userId) {
        notificationQueryRepository.dismissAll(userId);
    }

    private void requireOwnedNotification(
            Long notificationId,
            Long userId
    ) {
        if (!notificationQueryRepository.existsForReceiver(
                notificationId,
                userId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "알림을 찾을 수 없습니다."
            );
        }
    }
}
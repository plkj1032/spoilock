package com.spoilock.backend.notification.service;

import com.spoilock.backend.notification.domain.dto.NotificationListResponse;
import com.spoilock.backend.notification.domain.dto.NotificationResponse;
import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.repository.NotificationQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int PAGE_SIZE = 20;

    private final NotificationQueryRepository notificationQueryRepository;

    public NotificationListResponse getNotifications(
            Long userId,
            Long cursor
    ) {
        List<Notification> fetched =
                notificationQueryRepository.findPage(
                        userId,
                        cursor,
                        PAGE_SIZE + 1
                );

        boolean hasNext = fetched.size() > PAGE_SIZE;

        List<NotificationResponse> notifications = fetched.stream()
                .limit(PAGE_SIZE)
                .map(NotificationResponse::from)
                .toList();

        Long nextCursor = hasNext
                ? notifications.get(notifications.size() - 1)
                .notificationId()
                : null;

        return new NotificationListResponse(
                notifications,
                nextCursor,
                hasNext
        );
    }

    public long getUnreadCount(Long userId) {
        return notificationQueryRepository.countUnread(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        long updated = notificationQueryRepository.markAsRead(
                notificationId,
                userId
        );

        if (updated > 0) {
            return;
        }

        // 이미 읽은 본인 알림이면 정상 처리
        if (notificationQueryRepository.existsForReceiver(
                notificationId,
                userId
        )) {
            return;
        }

        // 존재하지 않거나 다른 사람의 알림인 경우
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "알림을 찾을 수 없습니다."
        );
    }
}
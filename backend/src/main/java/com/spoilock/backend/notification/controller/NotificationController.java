package com.spoilock.backend.notification.controller;

import com.spoilock.backend.notification.domain.dto.NotificationListResponse;
import com.spoilock.backend.notification.service.NotificationService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(
                    name = "cursor",
                    required = false
            )@Positive Long cursor
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(notificationService.getNotifications(userId, cursor));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt)
    {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        new UnreadCountResponse(
                                notificationService.getUnreadCount(userId)
                        )
                );
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(
            @PathVariable("notificationId") @Positive Long notificationId,
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = Long.valueOf(jwt.getSubject());
        notificationService.markAsRead(notificationId, userId);
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}

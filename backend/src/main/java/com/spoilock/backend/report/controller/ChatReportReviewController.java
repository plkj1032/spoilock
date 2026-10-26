package com.spoilock.backend.report.controller;

import com.spoilock.backend.report.dto.ChatReportReviewResponse;
import com.spoilock.backend.report.service.ChatReportReviewService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class ChatReportReviewController {
    private final ChatReportReviewService chatReportReviewService;

    @GetMapping("/{notificationId}/report-review")
    public ResponseEntity<ChatReportReviewResponse> getReview(
            @PathVariable("notificationId") @Positive Long notificationId,
            @AuthenticationPrincipal Jwt jwt
            ){
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        chatReportReviewService.getReview(notificationId, userId)
                );
    }
}

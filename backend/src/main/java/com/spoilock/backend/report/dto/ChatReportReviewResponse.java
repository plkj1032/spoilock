package com.spoilock.backend.report.dto;

import com.spoilock.backend.report.enums.ReportReason;

import java.time.LocalDateTime;
import java.util.List;

public record ChatReportReviewResponse(
        Long notificationId,
        Long roomId,
        String roomName,
        Long targetUserId,
        String targetNickname,
        boolean processed,
        List<ReportItem> reports
) {
    public record ReportItem(
            Long reportId,
            ReportReason reason,
            String reasonLabel,
            String detail,
            LocalDateTime createdAt,
            Long messageId,
            String messageContent,
            LocalDateTime messageCreatedAt
    ) {
    }
}
package com.spoilock.backend.report.dto;

import com.spoilock.backend.report.enums.ReportReason;

import java.time.LocalDateTime;
import java.util.List;

public record ChatReportReviewResponse(
        Long notificationId,
        Long roomId,
        String roomName,
        Long targetUserId,
        String targetNickName,
        boolean processed,
        List<ReportItem> reports
) {
    public record ReportItem(
            Long reporterId,
            ReportReason reason,
            String reasonLabel,
            String detail,
            LocalDateTime createdAt
    ){
    }
}

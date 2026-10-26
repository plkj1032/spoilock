package com.spoilock.backend.report.event;

public record ChatReportSubmitted(
        Long reportId,
        Long roomId,
        Long targetUserId,
        boolean thresholdReached
) {
}

package com.spoilock.backend.admin.monitoring;

import java.time.LocalDateTime;
import java.util.List;

/** 관리자 통계만 제공. 채팅 원문과 사용자 정보는 포함하지 않는다. */
public record ChatMonitoringResponse(
        String range,
        LocalDateTime from,
        LocalDateTime to,
        Summary summary,
        List<Bucket> timeline,
        List<Recent> recent,
        List<DramaOption> dramas
) {
    public record Summary(
            long total,
            long pending,
            long processing,
            long completed,
            long failed,
            long publicCount,
            long episodeGated,
            long held,
            Long averageDurationMs,
            Long p95DurationMs
    ) {}

    public record Bucket(
            String period,
            long total,
            long episodeGated,
            long failed
    ) {}

    public record Recent(
            Long messageId,
            Long dramaId,
            String dramaTitle,
            String content,
            String analysisStatus,
            String analysisOutcome,
            String policyCode,
            Long durationMs,
            LocalDateTime createdAt,
            LocalDateTime analyzedAt
    ) {}

    public record DramaOption(Long id, String title) {}
}

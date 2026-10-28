package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Python 자막 분석 작업 상태 (POST/GET /admin/subtitles/jobs).
 * status: QUEUED → PARSING → EXTRACTING → INDEXING → DONE / FAILED
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KbIngestJob(

        @JsonProperty("job_id")
        String jobId,

        @JsonProperty("status")
        String status,

        // 0.0 ~ 1.0
        @JsonProperty("progress")
        Double progress,

        @JsonProperty("done_batches")
        Integer doneBatches,

        @JsonProperty("total_batches")
        Integer totalBatches,

        // 완료 시 새로 색인된 사건 수
        @JsonProperty("event_count")
        Integer eventCount,

        @JsonProperty("kept_events")
        Integer keptEvents,

        @JsonProperty("error")
        String error
) {

    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";
    public static final String QUEUED = "QUEUED";

    public boolean isDone() {
        return DONE.equals(status);
    }

    public boolean isFailed() {
        return FAILED.equals(status);
    }

    /** 0 ~ 100 */
    public int progressPercent() {
        if (progress == null) {
            return 0;
        }
        return (int) Math.max(0, Math.min(100, Math.round(progress * 100)));
    }
}

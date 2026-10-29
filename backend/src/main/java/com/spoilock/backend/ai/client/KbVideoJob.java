package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Python 영상 분석 작업 상태 (POST/GET /admin/videos).
 * status: QUEUED → TRANSCRIBING → SRT_READY → INGESTING(사건 추출) → DONE / FAILED
 * progress 는 음성 인식 0~50%, 사건 추출 50~100%
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KbVideoJob(

        @JsonProperty("job_id")
        String jobId,

        @JsonProperty("status")
        String status,

        @JsonProperty("progress")
        Double progress,

        // 완료 시 새로 색인된 사건 수
        @JsonProperty("event_count")
        Integer eventCount,

        @JsonProperty("kept_events")
        Integer keptEvents,

        @JsonProperty("error")
        String error
) {

    /** 자막 분석 작업과 같은 형태로 변환 (상태 반영 로직 공유) */
    public KbIngestJob toIngestView() {
        String mapped = switch (status == null ? "" : status) {
            case "QUEUED" -> KbIngestJob.QUEUED;
            case "DONE" -> KbIngestJob.DONE;
            case "FAILED" -> KbIngestJob.FAILED;
            default -> "PROCESSING";   // TRANSCRIBING / SRT_READY / INGESTING
        };
        return new KbIngestJob(jobId, mapped, progress, null, null, eventCount, keptEvents, error);
    }
}

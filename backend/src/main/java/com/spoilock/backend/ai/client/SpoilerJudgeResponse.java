package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FastAPI POST /judge 응답 중 Spring이 사용하는 필드.
 * reason 은 사건 내용을 담을 수 있으므로 사용자 응답에 절대 넣지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpoilerJudgeResponse(

        @JsonProperty("message_id")
        Long messageId,

        // MATCH / NO_MATCH / UNCERTAIN / ERROR / SKIPPED_GENERAL / SKIPPED_UNRELATED
        @JsonProperty("analysis_verdict")
        String verdict,

        // Chroma 문서 ID (문자열). spoiler_events.chroma_doc_id 와 대응
        @JsonProperty("matched_event_id")
        String matchedEventId,

        @JsonProperty("required_episode_number")
        Integer requiredEpisodeNumber,

        // 현재 데모 데이터: UNVERIFIED_SOURCE_EPISODE_FOR_DEMO_ONLY
        @JsonProperty("episode_basis")
        String episodeBasis,

        @JsonProperty("needs_review")
        Boolean needsReview,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("error")
        String error
) {
}

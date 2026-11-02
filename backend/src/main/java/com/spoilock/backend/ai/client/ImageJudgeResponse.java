package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Python POST /admin/judge-image 응답 중 Spring 이 사용하는 필드.
 * reason 은 사건 내용을 담을 수 있으므로 사용자 응답에 넣지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageJudgeResponse(

        @JsonProperty("message_id")
        Long messageId,

        // MATCH / NO_MATCH / UNCERTAIN / ERROR / SKIPPED_NO_VIDEO_EVENTS / SKIPPED_NOT_SCENE
        @JsonProperty("analysis_verdict")
        String verdict,

        // 이 회차 이상 본 사람에게만 원본 (MATCH 일 때)
        @JsonProperty("required_episode_number")
        Integer requiredEpisodeNumber,

        @JsonProperty("matched_event_ids")
        List<String> matchedEventIds,

        @JsonProperty("episode_basis")
        String episodeBasis,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("error")
        String error
) {
}

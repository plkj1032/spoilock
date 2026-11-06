package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
        String error,

        // 메시지가 드라마 줄거리·인물 이야기인지 (Gemini 판단, 1차 필터 통과 시 false)
        // null 이면 알 수 없음 → 보수적으로 처리
        @JsonProperty("about_story")
        Boolean aboutStory,

        // 추측·질문이지만 사건 내용과 맞는 경우의 사건 ID 와 공개 회차.
        // 작성자가 그 회차를 이미 봤다면 '아는 내용을 추측처럼 말한 것' → 그 회차부터 공개
        @JsonProperty("guess_event_ids")
        List<String> guessEventIds,

        @JsonProperty("guess_episodes")
        List<Integer> guessEpisodes,

        // VERIFIED_FIRST_REVEAL / UNVERIFIED_SOURCE_EPISODE_FOR_DEMO_ONLY / null
        @JsonProperty("guess_basis")
        String guessBasis,

        @JsonProperty("matched_episodes")
        List<Integer> matchedEpisodes,

        // 채팅이 옮겨 말한 자막 대사의 회차 ("서윤하가 '…'라고 했잖아", 질문 속 인용 포함)
        //  작성자 진도와 상관없이 이 회차보다 덜 본 사람에게 가림
        @JsonProperty("quoted_line_episodes")
        List<Integer> quotedLineEpisodes
) {

    /** 추측 정보가 없는 응답 (이전 버전 Python 서버·테스트용) */
    public SpoilerJudgeResponse(
            Long messageId, String verdict, String matchedEventId, Integer requiredEpisodeNumber,
            String episodeBasis, Boolean needsReview, String reason, String error, Boolean aboutStory
    ) {
        this(messageId, verdict, matchedEventId, requiredEpisodeNumber, episodeBasis, needsReview,
                reason, error, aboutStory, null, null, null, null, null);
    }
    public SpoilerJudgeResponse(
            Long messageId,
            String verdict,
            String matchedEventId,
            Integer requiredEpisodeNumber,
            String episodeBasis,
            Boolean needsReview,
            String reason,
            String error,
            Boolean aboutStory,
            List<String> guessEventIds,
            List<Integer> guessEpisodes,
            String guessBasis
    ) {
        this(
                messageId, verdict, matchedEventId, requiredEpisodeNumber,
                episodeBasis, needsReview, reason, error, aboutStory,
                guessEventIds, guessEpisodes, guessBasis, null, null
        );
    }
}
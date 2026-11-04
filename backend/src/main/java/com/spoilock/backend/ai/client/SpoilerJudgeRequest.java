package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * FastAPI POST /judge 요청 본문 (Python 쪽 snake_case 필드명).
 * 사용자 시청 회차는 보내지 않는다. 공개 여부는 Spring이 사용자별로 판단한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpoilerJudgeRequest(

        @JsonProperty("message_id")
        Long messageId,

        @JsonProperty("message")
        String message,

        @JsonProperty("drama_id")
        Long dramaId,

        @JsonProperty("kb_version")
        String kbVersion,

        // 검색할 사건 회차 전체 범위
        @JsonProperty("episodes")
        List<Integer> episodes,

        @JsonProperty("retrieval")
        String retrieval,

        @JsonProperty("top_k")
        Integer topK,

        @JsonProperty("include_evidence")
        Boolean includeEvidence,

        @JsonProperty("reason_mode")
        String reasonMode,

        // null이면 생략 → Python 서버 기본값
        @JsonProperty("prefilter_mode")
        String prefilterMode,

        // 사건 설명(스포일러) 응답 금지
        @JsonProperty("debug")
        Boolean debug,

        // 직전 대화 (채팅만, 오래된 것 → 최근 순). null 이면 생략 → 메시지 한 줄만 판정
        @JsonProperty("context")
        List<ContextLine> context
) {

        /**
         * 같은 채팅방의 직전 메시지 1개. 판정 대상이 아니라 질문→답, 나눠 보낸 문장 해석용.
         */
        public record ContextLine(
                @JsonProperty("text")
                String text,

                // 지금 메시지 작성자와 같은 사람인지 (나눠 보낸 문장 판단)
                @JsonProperty("same_author")
                boolean sameAuthor,

                // 지금 메시지보다 몇 초 전에 보냈는지
                @JsonProperty("seconds_before")
                long secondsBefore
        ) {
        }
}
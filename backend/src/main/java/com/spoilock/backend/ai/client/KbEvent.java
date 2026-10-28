package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Python 사건 1건 (GET/POST/PATCH /admin/events). 사건 내용이 담기므로 관리자 화면에만 사용 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KbEvent(

        @JsonProperty("event_id")
        String eventId,

        @JsonProperty("description")
        String description,

        @JsonProperty("drama_id")
        Long dramaId,

        // Spring episodes.id (기존 데모 데이터는 101 같은 연습용 값일 수 있음)
        @JsonProperty("episode_id")
        Long episodeId,

        // 사건이 나온 회차 번호
        @JsonProperty("source_episode")
        Integer sourceEpisode,

        @JsonProperty("kb_version")
        String kbVersion,

        @JsonProperty("event_type")
        String eventType,

        @JsonProperty("fact_status")
        String factStatus,

        // PENDING / VERIFIED / REJECTED
        @JsonProperty("verification_status")
        String verificationStatus,

        // 미지정이면 null
        @JsonProperty("first_reveal_episode")
        Integer firstRevealEpisode,

        // AUTO / MANUAL / LEGACY
        @JsonProperty("origin")
        String origin,

        @JsonProperty("evidence")
        List<Evidence> evidence
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Evidence(
            @JsonProperty("document_id")
            String documentId,

            @JsonProperty("quote")
            String quote
    ) {
    }
}

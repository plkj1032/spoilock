package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Python GET /kb/episodes: 판정에 쓰이는(반려 제외) 사건이 있는 회차 번호 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KbEpisodes(

        @JsonProperty("drama_id")
        Long dramaId,

        @JsonProperty("kb_version")
        String kbVersion,

        @JsonProperty("episodes")
        List<Integer> episodes
) {
}

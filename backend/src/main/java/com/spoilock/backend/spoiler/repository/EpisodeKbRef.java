package com.spoilock.backend.spoiler.repository;

/** Python 사건 데이터와 연결할 회차 정보 (dramas.id, episodes.id, 회차 번호) */
public record EpisodeKbRef(
        Long episodeId,
        Long dramaId,
        Integer episodeNumber
) {
}

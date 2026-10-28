package com.spoilock.backend.spoiler.dto;

import com.spoilock.backend.ai.client.KbEvent;

import java.util.List;

/**
 * 관리자 화면용 사건 1건 (원본은 Python 사건 DB).
 * id / eventSummary / sourceType / verified / indexStatus 는 기존 화면 호환용.
 */
public record AdminSpoilerEventResponse(
        // Python 사건 ID (예: d1-e101-kb1-f0001)
        String id,
        String eventSummary,
        // 사건이 나온 회차 번호
        Integer sourceEpisode,
        // 최초 공개 회차 (미지정이면 null)
        Integer firstRevealEpisode,
        // PENDING / VERIFIED / REJECTED
        String verificationStatus,
        String eventType,
        String factStatus,
        // AUTO(자막 분석) / MANUAL(수동 등록) / LEGACY(기존 데이터)
        String origin,
        List<Evidence> evidence,

        // ---- 기존 화면 호환 ----
        String sourceType,
        boolean verified,
        String indexStatus
) {

    public record Evidence(String chunkId, String quote) {
    }

    public static AdminSpoilerEventResponse from(KbEvent event) {

        List<Evidence> evidence = event.evidence() == null
                ? List.of()
                : event.evidence().stream()
                .map(e -> new Evidence(e.documentId(), e.quote()))
                .toList();

        return new AdminSpoilerEventResponse(
                event.eventId(),
                event.description(),
                event.sourceEpisode(),
                event.firstRevealEpisode(),
                event.verificationStatus(),
                event.eventType(),
                event.factStatus(),
                event.origin(),
                evidence,
                "MANUAL".equals(event.origin()) ? "MANUAL" : "LICENSED_SUBTITLE",
                "VERIFIED".equals(event.verificationStatus()),
                // Python 사건 DB 에 저장된 사건은 곧바로 검색 대상
                "INDEXED"
        );
    }
}

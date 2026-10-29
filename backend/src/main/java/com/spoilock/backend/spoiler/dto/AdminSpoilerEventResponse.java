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
        // 화면 근거 (자막 + 영상 분석에서만): 관리자가 그 시각을 영상에서 직접 확인
        List<VisualEvidence> visualEvidence,
        // DIALOGUE(대사) / VISUAL(화면) / BOTH
        String evidenceType,

        // ---- 기존 화면 호환 ----
        String sourceType,
        boolean verified,
        String indexStatus
) {

    public record Evidence(String chunkId, String quote) {
    }

    public record VisualEvidence(String timestamp, String description) {
    }

    public static AdminSpoilerEventResponse from(KbEvent event) {

        List<Evidence> evidence = event.evidence() == null
                ? List.of()
                : event.evidence().stream()
                .map(e -> new Evidence(e.documentId(), e.quote()))
                .toList();

        List<VisualEvidence> visual = event.visualEvidence() == null
                ? List.of()
                : event.visualEvidence().stream()
                .map(v -> new VisualEvidence(v.timestamp(), v.description()))
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
                visual,
                event.evidenceType(),
                sourceType(event),
                "VERIFIED".equals(event.verificationStatus()),
                // Python 사건 DB 에 저장된 사건은 곧바로 검색 대상
                "INDEXED"
        );
    }

    /**
     * 출처: MANUAL(수동 등록) / SUBTITLE(자막) / SUBTITLE_VIDEO(자막 + 영상) / VIDEO_STT(영상 음성 인식)
     * 분석 원본 기록 이전의 기존 데이터는 LICENSED_SUBTITLE
     */
    private static String sourceType(KbEvent event) {
        if ("MANUAL".equals(event.origin())) {
            return "MANUAL";
        }
        return event.sourceMode() == null ? "LICENSED_SUBTITLE" : event.sourceMode();
    }
}

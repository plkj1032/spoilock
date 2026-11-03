package com.spoilock.backend.ai.service;

/**
 * AI 분석 결과를 chat_messages 상태로 옮기기 위한 결정.
 *
 * failed=true
 *  → analysis=FAILED
 *  → moderation=PENDING
 *
 * publish=true
 *  → analysis=COMPLETED
 *  → moderation=VISIBLE
 *
 * publish=false
 *  → analysis=COMPLETED
 *  → moderation=PENDING (관리자 검토)
 *
 * requiredEpisode=null
 *  → 모든 시청자에게 공개
 *
 * requiredEpisode=숫자
 *  → 해당 회차 이상 시청한 사용자에게 공개
 *
 * untilFinale=true
 *  → 드라마 마지막 회차까지 시청한 사용자에게만 공개
 */
public record AnalysisDecision(
        boolean failed,
        boolean publish,
        Integer requiredEpisode,
        String note,
        boolean untilFinale
) {

    // 기존 4개 인자 생성자와의 호환성 유지
    public AnalysisDecision(
            boolean failed,
            boolean publish,
            Integer requiredEpisode,
            String note
    ) {
        this(failed, publish, requiredEpisode, note, false);
    }

    // AI 분석 실패
    public static AnalysisDecision failed(String note) {
        return new AnalysisDecision(
                true, false, null, note, false
        );
    }

    // 관리자 검토 대기
    public static AnalysisDecision hold(String note) {
        return new AnalysisDecision(
                false, false, null, note, false
        );
    }

    // 전체 공개
    public static AnalysisDecision publishAll(String note) {
        return new AnalysisDecision(
                false, true, null, note, false
        );
    }

    // 특정 회차 이상 시청자에게 공개
    public static AnalysisDecision publishFrom(
            int episode,
            String note
    ) {
        return new AnalysisDecision(
                false, true, episode, note, false
        );
    }

    // 마지막 회차까지 시청한 사용자에게만 공개
    public static AnalysisDecision publishUntilFinale(
            int finaleEpisode,
            String note
    ) {
        return new AnalysisDecision(
                false, true, finaleEpisode, note, true
        );
    }
}

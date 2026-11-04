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
import com.spoilock.backend.ai.client.SpoilerJudgeResponse;

public record AnalysisDecision(
        boolean failed,
        boolean publish,
        Integer requiredEpisode,
        String note,
        boolean untilFinale,
        SpoilerJudgeResponse aiResponse
) {

    // 기존 5개 인자 호출 유지
    public AnalysisDecision(
            boolean failed,
            boolean publish,
            Integer requiredEpisode,
            String note,
            boolean untilFinale
    ) {
        this(
                failed, publish, requiredEpisode,
                note, untilFinale, null
        );
    }

    // 기존 4개 인자 호출 유지
    public AnalysisDecision(
            boolean failed,
            boolean publish,
            Integer requiredEpisode,
            String note
    ) {
        this(failed, publish, requiredEpisode, note, false, null);
    }

    public AnalysisDecision withAiResponse(SpoilerJudgeResponse response) {
        return new AnalysisDecision(
                failed,
                publish,
                requiredEpisode,
                note,
                untilFinale,
                response
        );
    }

    public static AnalysisDecision failed(String note) {
        return new AnalysisDecision(
                true, false, null, note, false
        );
    }

    public static AnalysisDecision hold(String note) {
        return new AnalysisDecision(
                false, false, null, note, false
        );
    }

    public static AnalysisDecision publishAll(String note) {
        return new AnalysisDecision(
                false, true, null, note, false
        );
    }

    public static AnalysisDecision publishFrom(
            int episode,
            String note
    ) {
        return new AnalysisDecision(
                false, true, episode, note, false
        );
    }

    public static AnalysisDecision publishUntilFinale(
            int finaleEpisode,
            String note
    ) {
        return new AnalysisDecision(
                false, true, finaleEpisode, note, true
        );
    }
}

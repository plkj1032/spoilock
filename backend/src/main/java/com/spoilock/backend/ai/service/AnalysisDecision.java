package com.spoilock.backend.ai.service;

/**
 * 분석 결과를 chat_messages 상태로 옮기기 위한 결정.
 *
 * <pre>
 * failed=true                → analysis=FAILED,    moderation=PENDING (원문 차단, 재시도 대상)
 * publish=true               → analysis=COMPLETED, moderation=VISIBLE, required_episode=requiredEpisode
 *                              (requiredEpisode=null 이면 전원 공개, 값이 있으면 그 회차 이상 시청자만)
 * publish=false              → analysis=COMPLETED, moderation=PENDING (관리자 검토 대기, 원문 차단)
 * untilFinale=true           → 판정이 애매해 '드라마 마지막 회차' 기준으로 공개 (회차 수 변경·새 사건 등록 시 갱신 대상)
 * </pre>
 */
public record AnalysisDecision(
        boolean failed,
        boolean publish,
        Integer requiredEpisode,
        String note,
        boolean untilFinale
) {

    public static AnalysisDecision failed(String note) {
        return new AnalysisDecision(true, false, null, note, false);
    }

    public static AnalysisDecision hold(String note) {
        return new AnalysisDecision(false, false, null, note, false);
    }

    public static AnalysisDecision publishAll(String note) {
        return new AnalysisDecision(false, true, null, note, false);
    }

    public static AnalysisDecision publishFrom(int episode, String note) {
        return new AnalysisDecision(false, true, episode, note, false);
    }

    /** 판정이 애매한 글: 드라마를 끝까지 본 사람에게만 공개 */
    public static AnalysisDecision publishUntilFinale(int finaleEpisode, String note) {
        return new AnalysisDecision(false, true, finaleEpisode, note, true);
    }
}

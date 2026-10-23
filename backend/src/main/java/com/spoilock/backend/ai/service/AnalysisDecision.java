package com.spoilock.backend.ai.service;

/**
 * 분석 결과를 chat_messages 상태로 옮기기 위한 결정.
 *
 * <pre>
 * failed=true                → analysis=FAILED,    moderation=PENDING (원문 차단, 재시도 대상)
 * publish=true               → analysis=COMPLETED, moderation=VISIBLE, required_episode=requiredEpisode
 *                              (requiredEpisode=null 이면 전원 공개, 값이 있으면 그 회차 이상 시청자만)
 * publish=false              → analysis=COMPLETED, moderation=PENDING (관리자 검토 대기, 원문 차단)
 * </pre>
 */
public record AnalysisDecision(
        boolean failed,
        boolean publish,
        Integer requiredEpisode,
        String note
) {

    public static AnalysisDecision failed(String note) {
        return new AnalysisDecision(true, false, null, note);
    }

    public static AnalysisDecision hold(String note) {
        return new AnalysisDecision(false, false, null, note);
    }

    public static AnalysisDecision publishAll(String note) {
        return new AnalysisDecision(false, true, null, note);
    }

    public static AnalysisDecision publishFrom(int episode, String note) {
        return new AnalysisDecision(false, true, episode, note);
    }
}

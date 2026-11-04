package com.spoilock.backend.ai.service;

/**
 * 관리자 모니터링용 판정 결과 코드 (회의 결정 11/3: 결과 4가지).
 *  - SPOILER      스포: 사건 회차부터 공개 (작성자가 본 회차 내용, 사건 일치)
 *  - NOT_SPOILER  스포 아님: 전체 공개 (잡담·감상·질문·다른 작품, 작성자가 안 본 회차)
 *  - HELD         보류: 판정을 확정 못 함 → 전체 공개 (app.spoiler.publish-held)
 *                 사건이 없는 작품도 판정할 수 없으므로 보류 (운영상 사건 있는 작품만 서비스)
 *  - FAILED       실패: 서버·AI 오류 → 재시도
 * 세부 이유는 AnalysisPolicyDetail 이 정해진 문구로 따로 저장한다.
 * decision.note() 원문이나 모델 reason/exception 메시지를 DB/API에 노출하지 않는다.
 *
 * 이전 버전 코드(EVENT_MATCH, GUESS_UNKNOWN, FINALE_ONLY 등)는 DB 의 예전 기록에만 남아 있고,
 * 관리자 화면이 그 이름을 그대로 표시한다.
 */
public enum AnalysisPolicyCode {
    SPOILER,
    NOT_SPOILER,
    HELD,
    FAILED;

    public static AnalysisPolicyCode from(AnalysisDecision decision) {
        if (decision == null || decision.failed()) return FAILED;

        String note = decision.note() == null ? "" : decision.note();

        if (decision.untilFinale()) return HELD;     // publish-held=false 일 때만 생김
        if (note.startsWith(HoldRules.HELD_PUBLISHED_PREFIX)) return HELD;
        if (note.startsWith("사건 미등록 작품")) return HELD;
        if (!decision.publish()) return HELD;
        return decision.requiredEpisode() != null ? SPOILER : NOT_SPOILER;
    }
}
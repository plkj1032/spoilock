package com.spoilock.backend.ai.service;

/**
 * 관리자 모니터링 전용 비민감 판정 사유 코드.
 * decision.note() 원문이나 모델 reason/exception 메시지를 DB/API에 노출하지 않는다.
 */
public enum AnalysisPolicyCode {
    GENERAL_CHAT,
    GENERAL_HELD,
    UNRELATED_CHAT,
    UNRELATED_HELD,
    EVENT_MATCH,
    MATCH_REVIEW,
    MATCH_INVALID,
    UNVERIFIED_EVENT,
    GUESS_KNOWN,
    FINALE_ONLY,
    GUESS_UNVERIFIED,
    NO_MATCH_PUBLISHED,
    OFF_TOPIC,
    NO_MATCH_REVIEW,
    UNCERTAIN_REVIEW,
    NO_EVENTS,
    KB_LOOKUP_FAILED,
    AI_REQUEST_FAILED,
    AI_SERVER_ERROR,
    EMPTY_VERDICT,
    UNKNOWN_VERDICT,
    OTHER_FAILED,
    OTHER_HELD,
    OTHER_PUBLIC;

    public static AnalysisPolicyCode from(AnalysisDecision decision) {
        if (decision == null) return OTHER_FAILED;
        if (decision.untilFinale()) return FINALE_ONLY;
        String note = decision.note();
        if (note == null) note = "";

        if (note.startsWith("1차 필터: 일반 대화(공개 설정 꺼짐)")) return GENERAL_HELD;
        if (note.startsWith("1차 필터: 일반 대화")) return GENERAL_CHAT;
        if (note.startsWith("1차 필터: 사건과 무관(공개 설정 꺼짐)")) return UNRELATED_HELD;
        if (note.startsWith("1차 필터: 사건과 무관")) return UNRELATED_CHAT;
        if (note.startsWith("NO_MATCH 공개 정책")) return NO_MATCH_PUBLISHED;
        if (note.startsWith("NO_MATCH: 작품과 무관한 대화")) return OFF_TOPIC;
        if (note.startsWith("NO_MATCH:")) return NO_MATCH_REVIEW;
        if (note.startsWith("UNCERTAIN:")) return UNCERTAIN_REVIEW;
        if (note.startsWith("MATCH 이지만 검토 필요")) return MATCH_REVIEW;
        if (note.startsWith("MATCH 인데") || note.startsWith("MATCH 회차가 색인 범위 밖")) return MATCH_INVALID;
        if (note.startsWith("MATCH ") && decision.publish() && decision.requiredEpisode() != null) return EVENT_MATCH;
        if (note.startsWith("미검증 사건 회차")) return UNVERIFIED_EVENT;
        if (note.startsWith("작성자가 본 회차의 추측이지만 미검증")) return GUESS_UNVERIFIED;
        if (note.startsWith("추측이지만 작성자가 이미 본")) return GUESS_KNOWN;
        if (note.startsWith("사건 미등록 작품")) return NO_EVENTS;
        if (note.startsWith("사건 DB 조회 실패")) return KB_LOOKUP_FAILED;
        if (note.startsWith("호출 실패:")) return AI_REQUEST_FAILED;
        if (note.startsWith("분석 서버 오류:")) return AI_SERVER_ERROR;
        if (note.startsWith("빈 판정")) return EMPTY_VERDICT;
        if (note.startsWith("알 수 없는 판정:")) return UNKNOWN_VERDICT;

        if (decision.failed()) return OTHER_FAILED;
        return decision.publish() ? OTHER_PUBLIC : OTHER_HELD;
    }
}

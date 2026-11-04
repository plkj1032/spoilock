package com.spoilock.backend.ai.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 관리자 모니터링용 판정 사유 (정해진 5가지 문구만 저장).
 * 사건 원문, Python reason, 예외 메세지는 저장하지 않는다.
 *
 *  결과        | 사유
 *  스포        | 사건 일치
 *  스포 아님   | 일반 대화 / 작성자 미시청 회차
 *  보류        | 판정 보류
 *  실패        | 분석 실패
 * 공개 회차·작성자 진도 같은 숫자는 '최종 공개 정책' 열(required_episode_number)에서 본다.
 */
public class AnalysisPolicyDetail {
    public static final String EVENT_MATCH = "사건 일치";
    public static final String GENERAL = "일반 대화";
    public static final String AUTHOR_NOT_WATCHED = "작성자 미시청 회차";
    public static final String HELD = "판정 보류";
    public static final String FAILED = "분석 실패";

    private AnalysisPolicyDetail() {
    }

    public static String from(AnalysisDecision decision) {
        if (decision == null) {
            return null;
        }
        String note = decision.note() == null ? "" : decision.note().trim();

        switch (AnalysisPolicyCode.from(decision)) {
            case FAILED:
                return FAILED;
            case HELD:
                return HELD;
            case SPOILER:
                return EVENT_MATCH;
            case NOT_SPOILER:
            default:
                break;
        }

        // 스포 아님: 왜 공개했는지
        if (note.startsWith("작성 당시 진도 이후 내용") || note.startsWith("안 본 회차 추측")) {
            return AUTHOR_NOT_WATCHED;
        }
        return GENERAL;   // 1차 필터, 감상·질문·잡담·다른 작품
    }
}
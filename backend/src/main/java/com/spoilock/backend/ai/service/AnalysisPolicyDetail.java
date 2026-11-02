package com.spoilock.backend.ai.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 관리자 모니터링용 안전한 정책 상세 설명,
 * 사건 원문, Python reason, 예외 메세지는 저장하지 않는다.
 */
public class AnalysisPolicyDetail {

    private static final Pattern UNKNOWN_GUESS = Pattern.compile(
            "^안 본 회차 추측: 작성자 ([0-9]{1,4})화, 추측 내용 ([0-9]{1,4})화 이후$"
    );

    private static final Pattern KNOWN_GUESS = Pattern.compile(
            "^추측이지만 작성자가 이미 본 ([0-9]{1,4})화 내용 \\(작성자 ([0-9]{1,4})화\\)$"
    );

    private AnalysisPolicyDetail() {}

    public static String from(AnalysisDecision decision)
    {
        if(decision == null || decision.note() == null)
        {
            return null;
        }

        String note = decision.note().trim();

        // 완전히 일치하는 안전한 형식만 저장
        Matcher unknown = UNKNOWN_GUESS.matcher(note);
        if(unknown.matches())
        {
            return "안 본 회차 추측: 작성자 "
                    + unknown.group(1)
                    + "화, 추측 내용 "
                    + unknown.group(2)
                    + "화 이후";
        }

        Matcher known = KNOWN_GUESS.matcher(note);
        if(known.matches())
        {
            return "본 회차 추측: 작성자 "
                    + known.group(2)
                    + "화, 추측 내용 "
                    + known.group(1)
                    + "화";
        }

        // 나머지는 기존 안전한 정책 코드로 표시
        return null;
    }

}

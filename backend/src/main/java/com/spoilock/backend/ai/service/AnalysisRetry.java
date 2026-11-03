package com.spoilock.backend.ai.service;

/**
 * 분석 실패 재시도 간격 계산.
 * 실패 1회 → base, 2회 → base×2, 3회 → base×4 ... 최대 1시간.
 * 예) base 30초, 최대 5회: 30초 → 1분 → 2분 → 4분 뒤 마지막 시도 (약 7~8분 동안 재시도)
 */
public final class AnalysisRetry {

    static final long MAX_DELAY_SECONDS = 3600;

    private AnalysisRetry() {
    }

    public static long delaySeconds(int failedAttempts, long baseDelaySeconds) {
        long base = Math.max(1, baseDelaySeconds);
        int exp = Math.max(0, Math.min(failedAttempts - 1, 20));
        return Math.min(MAX_DELAY_SECONDS, base << exp);
    }
}

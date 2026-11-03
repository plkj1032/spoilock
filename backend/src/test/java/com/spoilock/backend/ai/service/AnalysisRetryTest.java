package com.spoilock.backend.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalysisRetryTest {

    @Test
    void 실패할때마다_간격이_2배() {
        assertEquals(30, AnalysisRetry.delaySeconds(1, 30));
        assertEquals(60, AnalysisRetry.delaySeconds(2, 30));
        assertEquals(120, AnalysisRetry.delaySeconds(3, 30));
        assertEquals(240, AnalysisRetry.delaySeconds(4, 30));
    }

    @Test
    void 최대_1시간_그리고_이상한_값_방어() {
        assertEquals(3600, AnalysisRetry.delaySeconds(30, 30));
        assertEquals(30, AnalysisRetry.delaySeconds(0, 30));
        assertEquals(1, AnalysisRetry.delaySeconds(1, 0));
    }
}

package com.spoilock.backend.spoiler.repository;

import com.spoilock.backend.spoiler.enums.AnalysisSource;

/** 진행 중인 Python 자막 분석 작업 (상태 조회 대상) */
public record ActiveAnalysisJob(
        Long episodeId,
        String jobId,
        // null 은 V12 이전 자막 분석 → SUBTITLE 로 취급
        AnalysisSource source
) {
}

package com.spoilock.backend.spoiler.repository;

/** 진행 중인 Python 자막 분석 작업 (상태 조회 대상) */
public record ActiveAnalysisJob(
        Long episodeId,
        String jobId
) {
}

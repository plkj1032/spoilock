package com.spoilock.backend.spoiler.enums;

/** 회차 사건 분석의 원본 */
public enum AnalysisSource {
    // 관리자가 올린 SRT 자막 (Python /admin/subtitles/jobs)
    SUBTITLE,
    // 관리자가 올린 MP4 영상 → 음성 인식 → 자막 → 사건 추출 (Python /admin/videos)
    VIDEO
}

package com.spoilock.backend.spoiler.enums;

/** 회차 사건 분석의 원본 */
public enum AnalysisSource {
    // 관리자가 올린 SRT 자막 (Python /admin/subtitles/jobs)
    SUBTITLE,
    // SRT 자막 + 같은 회차 MP4 화면을 함께 분석 (Python /admin/subtitles/jobs, video 첨부)
    SUBTITLE_VIDEO,
    // 관리자가 올린 MP4 영상 → 음성 인식 → 자막 → 사건 추출 (Python /admin/videos)
    VIDEO
}

-- 회차 분석 원본 구분
-- SUBTITLE : 관리자가 올린 SRT 자막으로 분석 (Python /admin/subtitles/jobs)
-- VIDEO    : 관리자가 올린 MP4 영상을 음성 인식해 분석 (Python /admin/videos)
-- NULL     : 분석 요청 전 / 이 컬럼 추가 이전의 자막 분석
ALTER TABLE episode_analysis
    ADD COLUMN analysis_source VARCHAR(20) NULL;

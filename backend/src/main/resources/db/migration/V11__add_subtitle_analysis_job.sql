-- 관리자 자막 분석을 Python 작업으로 실행
-- analysis_job_id   : Python /admin/subtitles/jobs 의 job_id (진행 상태 조회·재시도용)
-- analysis_progress : 0 ~ 100 (%)
ALTER TABLE episode_analysis
    ADD COLUMN analysis_job_id VARCHAR(64) NULL,
    ADD COLUMN analysis_progress INT NULL;

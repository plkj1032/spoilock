-- AI 분석의 최종 시도 결과(재시도 시 갱신) 기록. 기존 메시지는 NULL 유지.
-- PUBLIC: 회차 제한 없는 공개 / EPISODE_GATED: 특정 회차 이후 공개
-- HELD: 관리자 검토 대기 / FAILED: 분석 실패
ALTER TABLE chat_messages
    ADD COLUMN analysis_outcome VARCHAR(24) NULL,
    ADD COLUMN analysis_duration_ms BIGINT NULL,
    ADD COLUMN analyzed_at DATETIME(6) NULL,
    ADD CONSTRAINT chk_chat_analysis_outcome
      CHECK (analysis_outcome IS NULL OR analysis_outcome IN
         ('PUBLIC', 'EPISODE_GATED', 'HELD', 'FAILED')),
    ADD CONSTRAINT chk_chat_analysis_duration
      CHECK (analysis_duration_ms IS NULL OR analysis_duration_ms >= 0),
    ADD INDEX idx_chat_monitor_type_created (message_type, created_at);

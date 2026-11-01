-- V19 적용 이후: 실제 분석 시도 단계별 기록 (재시도 이력 보존)
CREATE TABLE chat_analysis_trace (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    duration_ms BIGINT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_trace_message FOREIGN KEY (message_id)
        REFERENCES chat_messages(id) ON DELETE CASCADE,
    INDEX idx_trace_message_time (message_id, recorded_at),
    INDEX idx_trace_recorded (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_episode_analysis_requested
ON episode_analysis (analysis_requested_at, analysis_status);

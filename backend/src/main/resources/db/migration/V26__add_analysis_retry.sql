-- 스포일러 분석 실패 재시도를 '작성 후 10분'에서 '횟수 + 점점 늘어나는 간격'으로 변경
--  - analysis_attempts      : 분석 서버가 살아 있을 때 실패한 횟수 (꺼져 있는 동안은 시도하지 않아 늘지 않음)
--  - analysis_started_at    : 마지막 분석 시작 시각 (오래 멈춘 PROCESSING 판단)
--  - analysis_retry_at      : 다음 재시도 가능 시각
-- 기존 FAILED 글은 횟수 0 으로 시작하므로 배포 후 한 번씩 다시 판정됨
ALTER TABLE chat_messages
    ADD COLUMN analysis_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN analysis_started_at DATETIME(6) NULL,
    ADD COLUMN analysis_retry_at DATETIME(6) NULL;

ALTER TABLE posts
    ADD COLUMN analysis_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN analysis_started_at DATETIME(6) NULL,
    ADD COLUMN analysis_retry_at DATETIME(6) NULL;

CREATE INDEX idx_chat_messages_analysis_retry ON chat_messages (analysis_status, analysis_retry_at);
CREATE INDEX idx_posts_analysis_retry ON posts (analysis_status, analysis_retry_at);

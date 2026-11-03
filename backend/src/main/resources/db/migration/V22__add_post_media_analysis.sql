-- 게시글 이미지 스포일러 자동 판정 (영상 + 자막 분석 드라마)
-- analysis_status     : PENDING / PROCESSING / COMPLETED / FAILED
-- analysis_attempts   : 판정 시도 횟수 (실패 시 재시도 상한 확인용)
-- analysis_updated_at : 마지막 판정 시작·종료 시각 (멈춘 작업·재시도 간격 확인용)
-- analysis_note       : 판정 결과 메모 (관리자·로그용, 사용자에게 노출하지 않음)
ALTER TABLE post_media
    ADD COLUMN analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN analysis_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN analysis_updated_at DATETIME(6) NULL,
    ADD COLUMN analysis_note VARCHAR(255) NULL;

ALTER TABLE post_media
    ADD CONSTRAINT chk_post_media_analysis
        CHECK (analysis_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'SKIPPED'));

CREATE INDEX idx_post_media_analysis ON post_media (analysis_status, created_at);

-- 관리자 승인 대기로 숨겨져 있던 이미지 → 공개 상태로 두고 자동 판정 대상으로
-- (판정이 끝나기 전까지는 블러로 표시)
UPDATE post_media
   SET moderation_status = 'VISIBLE'
 WHERE moderation_status = 'PENDING';

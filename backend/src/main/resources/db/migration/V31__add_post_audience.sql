
-- 기존 게시글은 전체 공개 유지
ALTER TABLE posts
    ADD COLUMN audience VARCHAR(20)
        NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE posts
    ADD CONSTRAINT chk_post_audience
        CHECK (audience IN ('PUBLIC', 'FOLLOWERS'));


-- 게시글 및 댓글 수정 시 AI 분석의 구버전 결과가
-- 최신 본문을 덮어쓰지 않도록 수정 버전 관리

ALTER TABLE posts
    ADD COLUMN content_revision BIGINT NOT NULL DEFAULT 0;

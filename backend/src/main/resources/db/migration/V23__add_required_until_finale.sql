-- 판정이 애매해 '드라마 마지막 회차 기준'으로 공개 범위를 정한 글 표시
--  - 관리자가 작품의 전체 회차 수를 늘리면 공개 회차를 새 마지막 회차로 함께 변경
--  - 그 작품에 새 사건이 등록되면 최근 글을 다시 판정해 정확한 회차로 바꿈
ALTER TABLE chat_messages
    ADD COLUMN required_until_finale BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE posts
    ADD COLUMN required_until_finale BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_chat_messages_until_finale ON chat_messages (required_until_finale, created_at);
CREATE INDEX idx_posts_until_finale ON posts (required_until_finale, created_at);

ALTER TABLE chat_messages
    ADD COLUMN author_watched_episode_number INT NULL,
    ADD CONSTRAINT chk_chat_author_watched_episode
        CHECK (
            author_watched_episode_number IS NULL
            OR author_watched_episode_number >= 0
        );
CREATE TABLE episode_videos (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                episode_id BIGINT NOT NULL,

                                original_filename VARCHAR(255) NOT NULL,

                                storage_key VARCHAR(100) NOT NULL,

                                size_bytes BIGINT NOT NULL,

                                uploaded_at DATETIME(6)
        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                CONSTRAINT uk_episode_videos_episode
                                    UNIQUE (episode_id),

                                CONSTRAINT uk_episode_videos_storage_key
                                    UNIQUE (storage_key),

                                CONSTRAINT fk_episode_videos_episode
                                    FOREIGN KEY (episode_id)
                                        REFERENCES episodes(id),

                                CONSTRAINT chk_episode_videos_size
                                    CHECK (size_bytes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
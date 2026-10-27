CREATE TABLE post_media (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            post_id BIGINT NOT NULL,

                            storage_key VARCHAR(80) NOT NULL,
                            original_filename VARCHAR(255) NOT NULL,
                            media_type VARCHAR(30) NOT NULL,
                            size_bytes BIGINT NOT NULL,
                            sort_order INT NOT NULL,

                            moderation_status VARCHAR(20)
                                NOT NULL DEFAULT 'PENDING',

                            required_episode_number INT NULL,

                            created_at DATETIME(6)
        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                            CONSTRAINT fk_post_media_post
                                FOREIGN KEY (post_id)
                                    REFERENCES posts(id),

                            CONSTRAINT uk_post_media_storage
                                UNIQUE (storage_key),

                            CONSTRAINT uk_post_media_order
                                UNIQUE (post_id, sort_order),

                            CONSTRAINT chk_post_media_size
                                CHECK (size_bytes > 0),

                            CONSTRAINT chk_post_media_order
                                CHECK (sort_order >= 0),

                            CONSTRAINT chk_post_media_status
                                CHECK (
                                    moderation_status IN (
                                                          'PENDING', 'VISIBLE', 'HIDDEN', 'REMOVED'
                                        )
                                    ),

                            CONSTRAINT chk_post_media_episode
                                CHECK (
                                    required_episode_number IS NULL
                                        OR required_episode_number > 0
                                    ),

                            INDEX idx_post_media_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
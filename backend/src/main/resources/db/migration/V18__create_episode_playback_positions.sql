CREATE TABLE episode_playback_positions (
                                            id BIGINT NOT NULL AUTO_INCREMENT,
                                            user_id BIGINT NOT NULL,
                                            episode_id BIGINT NOT NULL,
                                            position_seconds DOUBLE NOT NULL DEFAULT 0,
                                            duration_seconds DOUBLE NOT NULL,
                                            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                            PRIMARY KEY (id),

                                            CONSTRAINT uk_playback_user_episode
                                                UNIQUE (user_id, episode_id),

                                            CONSTRAINT fk_playback_position_user
                                                FOREIGN KEY (user_id)
                                                    REFERENCES users(id)
                                                    ON DELETE CASCADE,

                                            CONSTRAINT fk_playback_position_episode
                                                FOREIGN KEY (episode_id)
                                                    REFERENCES episodes(id)
                                                    ON DELETE CASCADE,

                                            CONSTRAINT chk_playback_duration
                                                CHECK (duration_seconds > 0),

                                            CONSTRAINT chk_playback_position
                                                CHECK (
                                                    position_seconds >= 0
                                                        AND position_seconds <= duration_seconds
                                                    ),

                                            INDEX idx_playback_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE drama_ratings (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               drama_id BIGINT NOT NULL,
                               score INT NOT NULL,

                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                               PRIMARY KEY (id),

                               CONSTRAINT uk_drama_rating_user_drama
                                   UNIQUE (user_id, drama_id),

                               INDEX idx_drama_rating_drama (drama_id),

                               CONSTRAINT fk_drama_rating_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id),

                               CONSTRAINT fk_drama_rating_drama
                                   FOREIGN KEY (drama_id)
                                       REFERENCES dramas(id),

                               CONSTRAINT chk_drama_rating_score
                                   CHECK (score BETWEEN 0 AND 10)
);
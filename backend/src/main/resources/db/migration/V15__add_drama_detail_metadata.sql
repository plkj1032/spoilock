ALTER TABLE dramas
    ADD COLUMN premiere_date DATE NULL,
    ADD COLUMN directors VARCHAR(500) NULL,
    ADD COLUMN writers VARCHAR(500) NULL,
    ADD COLUMN cast_names VARCHAR(2000) NULL;

CREATE TABLE drama_genres (
                              drama_id BIGINT NOT NULL,
                              genre VARCHAR(30) NOT NULL,

                              PRIMARY KEY (drama_id, genre),

                              CONSTRAINT fk_drama_genre_drama
                                  FOREIGN KEY (drama_id)
                                      REFERENCES dramas(id)
                                      ON DELETE CASCADE
);
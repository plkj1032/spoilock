CREATE TABLE chat_invite_links (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   room_id BIGINT NOT NULL,
                                   issuer_id BIGINT NOT NULL,
                                   code_hash VARCHAR(64) NOT NULL,
                                   expires_at DATETIME(6) NOT NULL,
                                   revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                   max_uses INT NOT NULL,
                                   used_count INT NOT NULL DEFAULT 0,

                                   PRIMARY KEY (id),

                                   CONSTRAINT uk_chat_link_hash
                                       UNIQUE (code_hash),

                                   CONSTRAINT fk_chat_link_room
                                       FOREIGN KEY (room_id)
                                           REFERENCES chat_rooms(id),

                                   CONSTRAINT fk_chat_link_issuer
                                       FOREIGN KEY (issuer_id)
                                           REFERENCES users(id),

                                   CONSTRAINT chk_chat_link_uses
                                       CHECK (
                                           max_uses BETWEEN 1 AND 100
                                               AND used_count BETWEEN 0 AND max_uses
                                           ),

                                   INDEX idx_chat_link_room (room_id, id)
);

CREATE TABLE chat_invite_link_uses (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       link_id BIGINT NOT NULL,
                                       user_id BIGINT NOT NULL,

                                       PRIMARY KEY (id),

                                       CONSTRAINT uk_chat_link_user
                                           UNIQUE (link_id, user_id),

                                       CONSTRAINT fk_chat_link_use_link
                                           FOREIGN KEY (link_id)
                                               REFERENCES chat_invite_links(id),

                                       CONSTRAINT fk_chat_link_use_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users(id)
);
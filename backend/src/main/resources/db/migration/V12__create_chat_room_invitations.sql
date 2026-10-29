CREATE TABLE chat_room_invitations (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       room_id BIGINT NOT NULL,
                                       sender_user_id BIGINT NOT NULL,
                                       receiver_user_id BIGINT NOT NULL,
                                       code_hash VARCHAR(64) NOT NULL,
                                       status VARCHAR(20) NOT NULL,
                                       expires_at DATETIME(6) NOT NULL,
                                       responded_at DATETIME(6) NULL,
                                       created_at DATETIME(6) NOT NULL,

                                       PRIMARY KEY (id),

                                       CONSTRAINT uk_chat_invitation_code_hash
                                           UNIQUE (code_hash),

                                       CONSTRAINT fk_chat_invitation_room
                                           FOREIGN KEY (room_id) REFERENCES chat_rooms(id),

                                       CONSTRAINT fk_chat_invitation_sender
                                           FOREIGN KEY (sender_user_id) REFERENCES users(id),

                                       CONSTRAINT fk_chat_invitation_receiver
                                           FOREIGN KEY (receiver_user_id) REFERENCES users(id),

                                       CONSTRAINT chk_chat_invitation_status
                                           CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),

                                       CONSTRAINT chk_chat_invitation_users
                                           CHECK (sender_user_id <> receiver_user_id),

                                       INDEX idx_chat_invitation_pending (
        room_id,
        receiver_user_id,
        status,
        expires_at
    ),

                                       INDEX idx_chat_invitation_receiver (
        receiver_user_id,
        created_at
    )
);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notification_invitation
        FOREIGN KEY (invitation_id)
            REFERENCES chat_room_invitations(id);
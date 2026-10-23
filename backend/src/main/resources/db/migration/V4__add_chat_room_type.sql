ALTER TABLE chat_rooms
    ADD COLUMN room_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOM',
    ADD CONSTRAINT chk_chat_room_type
        CHECK (room_type IN ('DEFAULT', 'CUSTOM', 'PRIVATE'));
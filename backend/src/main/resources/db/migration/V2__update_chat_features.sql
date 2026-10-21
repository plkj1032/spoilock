-- =========================================================
-- V2__update_chat_features.sql
-- 채팅 기능 확장
-- =========================================================


-- 1. CHAT_MESSAGES.message_type 추가
ALTER TABLE chat_messages
    ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT 'CHAT'
    AFTER user_id;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_message_type
        CHECK (
            message_type IN (
                             'CHAT',
                             'ENTER',
                             'LEAVE',
                             'KICK'
                )
            );


-- 2. analysis_status에 SKIPPED 추가
ALTER TABLE chat_messages
DROP CHECK chk_chat_analysis_status;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_analysis_status
        CHECK (
            analysis_status IN (
                                'PENDING',
                                'PROCESSING',
                                'COMPLETED',
                                'FAILED',
                                'SKIPPED'
                )
            );


-- 3. moderation_status를 Java Enum과 통일
-- Java:
-- PENDING / VISIBLE / HIDDEN / REMOVED

ALTER TABLE chat_messages
DROP CHECK chk_chat_moderation_status;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_moderation_status
        CHECK (
            moderation_status IN (
                                  'PENDING',
                                  'VISIBLE',
                                  'HIDDEN',
                                  'REMOVED'
                )
            );


-- 4. 채팅방 강퇴 / 강퇴 취소 이력 테이블
CREATE TABLE chat_room_bans (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                room_id BIGINT NOT NULL,
                                user_id BIGINT NOT NULL,

                                kicked_by BIGINT NOT NULL,
                                reason VARCHAR(500) NULL,

                                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                released_at DATETIME(6) NULL,
                                released_by BIGINT NULL,

                                CONSTRAINT fk_chat_ban_room
                                    FOREIGN KEY (room_id)
                                        REFERENCES chat_rooms(id),

                                CONSTRAINT fk_chat_ban_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id),

                                CONSTRAINT fk_chat_ban_kicker
                                    FOREIGN KEY (kicked_by)
                                        REFERENCES users(id),

                                CONSTRAINT fk_chat_ban_releaser
                                    FOREIGN KEY (released_by)
                                        REFERENCES users(id),

                                CONSTRAINT chk_chat_ban_status
                                    CHECK (
                                        status IN (
                                                   'ACTIVE',
                                                   'RELEASED'
                                            )
                                        ),

                                INDEX idx_chat_ban_room_user_status (
        room_id,
        user_id,
        status
    ),

                                INDEX idx_chat_ban_status (
        status
    )

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
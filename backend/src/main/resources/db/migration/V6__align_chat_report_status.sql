-- 1. 기존 상태 제약 제거
ALTER TABLE chat_reports
DROP CHECK chk_chat_report_status;

-- 2. 기존 데이터의 상태를 Java Enum에 맞춤
UPDATE chat_reports
SET status = CASE status
                 WHEN 'OPEN' THEN 'PENDING'
                 WHEN 'RESOLVED' THEN 'APPROVED'
                 WHEN 'DISMISSED' THEN 'REJECTED'
                 ELSE status
    END;

-- 3. 기본값과 허용 상태 통일
ALTER TABLE chat_reports
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE chat_reports
    ADD CONSTRAINT chk_chat_report_status
        CHECK (
            status IN ('PENDING', 'APPROVED', 'REJECTED')
            );

-- 4. 같은 사람이 같은 메시지를 중복 신고하지 못하도록 보장
ALTER TABLE chat_reports
    ADD CONSTRAINT uk_chat_report_user
        UNIQUE (message_id, reporter_id);

ALTER TABLE chat_reports
    ADD COLUMN detail VARCHAR(500) NULL AFTER reason;

CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,

                               receiver_id BIGINT NOT NULL,
                               notification_type VARCHAR(30) NOT NULL,

                               room_id BIGINT NOT NULL,
                               target_user_id BIGINT NOT NULL,
                               source_report_id BIGINT NOT NULL,

                               content VARCHAR(1000) NOT NULL,

                               read_at DATETIME(6) NULL,
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               CONSTRAINT fk_notification_receiver
                                   FOREIGN KEY (receiver_id)
                                       REFERENCES users(id),

                               CONSTRAINT fk_notification_room
                                   FOREIGN KEY (room_id)
                                       REFERENCES chat_rooms(id),

                               CONSTRAINT fk_notification_target
                                   FOREIGN KEY (target_user_id)
                                       REFERENCES users(id),

                               CONSTRAINT fk_notification_report
                                   FOREIGN KEY (source_report_id)
                                       REFERENCES chat_reports(id),

                               CONSTRAINT chk_notification_type
                                   CHECK (
                                       notification_type IN (
                                                             'CHAT_REPORT_RECEIVED',
                                                             'CHAT_REPORT_THRESHOLD'
                                           )
                                       ),

                               CONSTRAINT uk_notification_receiver_report_type
                                   UNIQUE (
                                           receiver_id,
                                           source_report_id,
                                           notification_type
                                       ),

                               INDEX idx_notification_receiver_created (
        receiver_id,
        created_at,
        id
    ),

                               INDEX idx_notification_receiver_read (
        receiver_id,
        read_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
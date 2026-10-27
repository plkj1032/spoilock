-- 신고 알림에만 필요한 연결 정보는 선택값으로 변경
ALTER TABLE notifications
    MODIFY COLUMN room_id BIGINT NULL,
    MODIFY COLUMN target_user_id BIGINT NULL,
    MODIFY COLUMN source_report_id BIGINT NULL;

-- 공통 발신자, 초대 연결 정보, 알림창 숨김 상태
ALTER TABLE notifications
    ADD COLUMN sender_user_id BIGINT NULL,
    ADD COLUMN invitation_id BIGINT NULL,
    ADD COLUMN dismissed_at DATETIME(6) NULL,
    ADD CONSTRAINT fk_notification_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(id);

-- 지원하는 알림 종류
ALTER TABLE notifications
DROP CHECK chk_notification_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notification_type
        CHECK (
            notification_type IN (
                                  'CHAT_REPORT_RECEIVED',
                                  'CHAT_REPORT_THRESHOLD',
                                  'FOLLOW_RECEIVED',
                                  'CHAT_ROOM_INVITATION'
                )
            );

-- 하나의 초대에 같은 수신자의 알림이 중복 생성되는 것 방지
ALTER TABLE notifications
    ADD CONSTRAINT uk_notification_receiver_invitation_type
        UNIQUE (
                receiver_id,
                invitation_id,
                notification_type
            );

-- 숨기지 않은 최신 알림 조회용
CREATE INDEX idx_notification_receiver_dismissed_id
    ON notifications (
                      receiver_id,
                      dismissed_at,
                      id
        );
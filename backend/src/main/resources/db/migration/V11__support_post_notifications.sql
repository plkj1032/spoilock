ALTER TABLE notifications
DROP CHECK chk_notification_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notification_type
        CHECK (
            notification_type IN (
                                  'CHAT_REPORT_RECEIVED',
                                  'CHAT_REPORT_THRESHOLD',
                                  'FOLLOW_RECEIVED',
                                  'CHAT_ROOM_INVITATION',
                                  'POST_COMMENT_RECEIVED',
                                  'POST_LIKE_RECEIVED'
                )
            );
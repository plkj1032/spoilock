-- V19/V20은 수정하지 않는다. 기존 메시지의 정책 코드는 NULL로 남는다.
ALTER TABLE chat_messages
    ADD COLUMN analysis_policy_code VARCHAR(40) NULL;

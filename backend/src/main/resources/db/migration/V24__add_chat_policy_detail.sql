-- AI 판정의 안전한 상세 정책 사유
-- journal 원문 전체나 오류 메세지를 저장하지 않음

ALTER TABLE chat_messages
ADD COLUMN analysis_policy_detail VARCHAR(255) NULL;
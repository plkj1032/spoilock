
-- SPOILOCK V3
-- SNS 검열 상태를 공통 Java Enum과 통일

ALTER TABLE posts
DROP CHECK chk_post_moderation_status;

-- 기존 데이터가 있을 경우 상태값 변환
UPDATE posts
SET moderation_status =
        CASE moderation_status
            WHEN 'APPROVED' THEN 'VISIBLE'
            WHEN 'REVIEW' THEN 'HIDDEN'
            WHEN 'REJECTED' THEN 'REMOVED'
            ELSE moderation_status
            END
WHERE moderation_status IN (
                            'APPROVED', 'REVIEW', 'REJECTED'
    );

ALTER TABLE posts
    ADD CONSTRAINT chk_post_moderation_status
        CHECK (
            moderation_status IN (
                                  'PENDING',
                                  'VISIBLE',
                                  'HIDDEN',
                                  'REMOVED'
                )
            );

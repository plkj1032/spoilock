CREATE TABLE copyright_license_histories (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                             license_id BIGINT NOT NULL,

    -- CREATED: 최초 등록, UPDATED: 수정
                                             action VARCHAR(20) NOT NULL,

    -- 변경을 수행한 관리자와 당시 닉네임
                                             actor_id BIGINT NOT NULL,
                                             actor_nickname VARCHAR(255) NOT NULL,

    -- 변경 전후 데이터를 JSON 문자열로 보관
                                             before_snapshot LONGTEXT NULL,
                                             after_snapshot LONGTEXT NOT NULL,

                                             created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                             CONSTRAINT fk_copyright_history_license
                                                 FOREIGN KEY (license_id)
                                                     REFERENCES copyright_licenses(id),

                                             CONSTRAINT fk_copyright_history_actor
                                                 FOREIGN KEY (actor_id)
                                                     REFERENCES users(id),

                                             CONSTRAINT chk_copyright_history_action
                                                 CHECK (action IN ('CREATED', 'UPDATED')),

    CONSTRAINT chk_copyright_history_before_json
        CHECK (
            before_snapshot IS NULL
            OR JSON_VALID(before_snapshot)
        ),

    CONSTRAINT chk_copyright_history_after_json
        CHECK (JSON_VALID(after_snapshot)),

    CONSTRAINT chk_copyright_history_snapshots
        CHECK (
            (action = 'CREATED' AND before_snapshot IS NULL)
            OR
            (action = 'UPDATED' AND before_snapshot IS NOT NULL)
        ),

    INDEX idx_copyright_history_license (
        license_id,
        created_at,
        id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
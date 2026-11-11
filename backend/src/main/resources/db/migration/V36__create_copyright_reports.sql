CREATE TABLE copyright_reports (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 신고한 회원
                                   reporter_id BIGINT NOT NULL,

    -- 신고 대상 유형
    -- EPISODE_VIDEO: 회차 영상
    -- EPISODE_SUBTITLE: 회차 자막
    -- POST: 게시글
    -- POST_MEDIA: 게시글 첨부 이미지
                                   target_type VARCHAR(30) NOT NULL,

    -- 대상 유형에 따른 콘텐츠 ID
                                   target_id BIGINT NOT NULL,

    -- 접수 당시 대상 정보
    -- 서버가 대상 조회 후 생성하는 JSON
                                   target_snapshot LONGTEXT NOT NULL,

    -- 신고자가 입력한 권리자 및 연락 정보
                                   claimant_name VARCHAR(200) NOT NULL,
                                   contact_email VARCHAR(255) NOT NULL,

    -- 어떤 저작물에 대한 권리인지
                                   original_work_description TEXT NOT NULL,

    -- 침해라고 판단하는 사유 및 해당 부분
                                   infringement_description TEXT NOT NULL,

    -- 접수 및 검토 상태
                                   status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',

    -- 마지막 처리 관리자
                                   handled_by BIGINT NULL,

    -- 검토 또는 처리 사유
                                   review_note TEXT NULL,

    -- 최종 처리 시각
                                   resolved_at DATETIME(6) NULL,

    -- 동시 처리 충돌 방지
                                   version BIGINT NOT NULL DEFAULT 0,

                                   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                   CONSTRAINT fk_copyright_report_reporter
                                       FOREIGN KEY (reporter_id)
                                           REFERENCES users(id),

                                   CONSTRAINT fk_copyright_report_handler
                                       FOREIGN KEY (handled_by)
                                           REFERENCES users(id),

                                   CONSTRAINT chk_copyright_report_target_type
                                       CHECK (
                                           target_type IN (
                                                           'EPISODE_VIDEO',
                                                           'EPISODE_SUBTITLE',
                                                           'POST',
                                                           'POST_MEDIA'
                                               )
                                           ),

                                   CONSTRAINT chk_copyright_report_target_id
                                       CHECK (target_id > 0),

                                   CONSTRAINT chk_copyright_report_snapshot
                                       CHECK (JSON_VALID(target_snapshot)),

                                   CONSTRAINT chk_copyright_report_status
                                       CHECK (
                                           status IN (
                                                      'RECEIVED',
                                                      'REVIEWING',
                                                      'RESOLVED',
                                                      'REJECTED'
                                               )
                                           ),

                                   CONSTRAINT chk_copyright_report_version
                                       CHECK (version >= 0),

    -- 최종 처리된 신고에만 완료 시각을 기록
                                   CONSTRAINT chk_copyright_report_resolution
                                       CHECK (
                                           (
                                               status IN ('RECEIVED', 'REVIEWING')
                                                   AND resolved_at IS NULL
                                               )
                                               OR
                                           (
                                               status IN ('RESOLVED', 'REJECTED')
                                                   AND resolved_at IS NOT NULL
                                               )
                                           ),

    -- 검토 이후 단계에는 담당자와 사유가 필요
                                   CONSTRAINT chk_copyright_report_review
                                       CHECK (
                                           status = 'RECEIVED'
                                               OR (
                                               handled_by IS NOT NULL
                                                   AND review_note IS NOT NULL
                                                   AND CHAR_LENGTH(TRIM(review_note)) > 0
                                               )
                                           ),

                                   INDEX idx_copyright_report_status (
        status,
        created_at,
        id
    ),

                                   INDEX idx_copyright_report_reporter (
        reporter_id,
        created_at,
        id
    ),

                                   INDEX idx_copyright_report_target (
        target_type,
        target_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE copyright_report_histories (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                            report_id BIGINT NOT NULL,

                                            from_status VARCHAR(20) NOT NULL,
                                            to_status VARCHAR(20) NOT NULL,

                                            actor_id BIGINT NOT NULL,
                                            actor_nickname VARCHAR(255) NOT NULL,

                                            review_note TEXT NOT NULL,

    -- 이번 처리 후 신고의 버전
                                            report_version BIGINT NOT NULL,

                                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                            CONSTRAINT fk_copyright_report_history_report
                                                FOREIGN KEY (report_id)
                                                    REFERENCES copyright_reports(id),

                                            CONSTRAINT fk_copyright_report_history_actor
                                                FOREIGN KEY (actor_id)
                                                    REFERENCES users(id),

                                            CONSTRAINT uk_copyright_report_history_version
                                                UNIQUE (report_id, report_version),

                                            CONSTRAINT chk_copyright_report_history_transition
                                                CHECK (
                                                    (from_status = 'RECEIVED' AND to_status = 'REVIEWING')
                                                        OR
                                                    (
                                                        from_status = 'REVIEWING'
                                                            AND to_status IN ('RESOLVED', 'REJECTED')
                                                        )
                                                    ),

                                            CONSTRAINT chk_copyright_report_history_note
                                                CHECK (CHAR_LENGTH(TRIM(review_note)) > 0),

                                            CONSTRAINT chk_copyright_report_history_version
                                                CHECK (report_version >= 0),

                                            INDEX idx_copyright_report_history_report (
        report_id,
        created_at,
        id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
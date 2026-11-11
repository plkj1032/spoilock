-- 작품별 이용 권한 관리
-- 한 작품에 계약·권리자가 여러 개일 수 있으므로 drama_id는 UNIQUE로 제한하지 않음

CREATE TABLE copyright_licenses (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 대상 작품
                                    drama_id BIGINT NOT NULL,

    -- 계약 또는 이용 허락 건 이름
                                    license_name VARCHAR(200) NOT NULL,

    -- 권리자 및 연락 정보
                                    rights_holder VARCHAR(200) NOT NULL,
                                    contact_name VARCHAR(100) NULL,
                                    contact_email VARCHAR(255) NULL,

    -- 관리자 확인 상태
    -- DRAFT: 작성 중
    -- REVIEWING: 검토 중
    -- CONFIRMED: 확인 완료
    -- REVOKED: 철회·해지
                                    review_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- 용도별 이용 허락 기록
    -- UNCONFIRMED: 미확인
    -- ALLOWED: 허용
    -- NOT_ALLOWED: 미허용
                                    video_permission VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',
                                    subtitle_permission VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',
                                    ai_permission VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',

    -- 계약에 기재된 이용 기간
                                    starts_on DATE NULL,
                                    ends_on DATE NULL,

    -- 허용 지역·대상 회차·기타 제한 등
                                    scope_notes TEXT NULL,

    -- 관리자 메모
                                    admin_notes TEXT NULL,

    -- 최초 등록 및 마지막 수정 담당자
                                    created_by BIGINT NOT NULL,
                                    updated_by BIGINT NOT NULL,

    -- 동시 수정 충돌 방지용 버전
                                    version BIGINT NOT NULL DEFAULT 0,

                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                    CONSTRAINT fk_copyright_license_drama
                                        FOREIGN KEY (drama_id) REFERENCES dramas(id),

                                    CONSTRAINT fk_copyright_license_creator
                                        FOREIGN KEY (created_by) REFERENCES users(id),

                                    CONSTRAINT fk_copyright_license_updater
                                        FOREIGN KEY (updated_by) REFERENCES users(id),

                                    CONSTRAINT chk_copyright_license_status
                                        CHECK (
                                            review_status IN (
                                                              'DRAFT',
                                                              'REVIEWING',
                                                              'CONFIRMED',
                                                              'REVOKED'
                                                )
                                            ),

                                    CONSTRAINT chk_copyright_video_permission
                                        CHECK (
                                            video_permission IN (
                                                                 'UNCONFIRMED',
                                                                 'ALLOWED',
                                                                 'NOT_ALLOWED'
                                                )
                                            ),

                                    CONSTRAINT chk_copyright_subtitle_permission
                                        CHECK (
                                            subtitle_permission IN (
                                                                    'UNCONFIRMED',
                                                                    'ALLOWED',
                                                                    'NOT_ALLOWED'
                                                )
                                            ),

                                    CONSTRAINT chk_copyright_ai_permission
                                        CHECK (
                                            ai_permission IN (
                                                              'UNCONFIRMED',
                                                              'ALLOWED',
                                                              'NOT_ALLOWED'
                                                )
                                            ),

                                    CONSTRAINT chk_copyright_license_dates
                                        CHECK (
                                            starts_on IS NULL
                                                OR ends_on IS NULL
                                                OR ends_on >= starts_on
                                            ),

                                    CONSTRAINT chk_copyright_license_version
                                        CHECK (version >= 0),

                                    INDEX idx_copyright_license_drama (drama_id),
                                    INDEX idx_copyright_license_status (review_status),
                                    INDEX idx_copyright_license_expiry (ends_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
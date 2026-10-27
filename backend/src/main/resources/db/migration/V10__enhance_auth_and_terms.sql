
-- =====================================================
-- SPOILOCK V10
-- 회원가입 보안 강화 및 약관 버전 관리
-- MySQL 8.0
-- =====================================================

-- =====================================================
-- 1. USERS 확장
-- =====================================================

ALTER TABLE users
    -- 이메일 소유 확인 완료 시각
    ADD COLUMN email_verified_at DATETIME(6) NULL,

    -- 닉네임 중복 검사에 사용하는 정규화 키
    -- 원본 nickname은 변경하지 않음
    ADD COLUMN nickname_key VARCHAR(50)
        GENERATED ALWAYS AS (
            LOWER(TRIM(nickname))
        ) STORED,

    -- 대소문자/앞뒤 공백 차이를 포함한 중복 방지
    ADD CONSTRAINT uk_users_nickname_key
        UNIQUE (nickname_key);


-- =====================================================
-- 2. EMAIL_VERIFICATIONS
-- 이메일 인증번호 발급 및 검증 상태
-- =====================================================

CREATE TABLE email_verifications (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 소문자/공백 정규화한 이메일
                                     email VARCHAR(255) NOT NULL,

    -- 6자리 인증번호의 HMAC-SHA256 값
                                     code_hash CHAR(64) NOT NULL,

    -- 인증 성공 후 발급하는
    -- 일회성 증명 토큰의 SHA-256 해시
                                     verification_token_hash CHAR(64) NULL,

    -- 인증번호 만료 시각
                                     code_expires_at DATETIME(6) NOT NULL,

    -- 인증 성공 시각
                                     verified_at DATETIME(6) NULL,

    -- 회원가입용 증명 토큰 만료 시각
                                     proof_expires_at DATETIME(6) NULL,

    -- 회원가입에서 증명 토큰 사용한 시각
                                     consumed_at DATETIME(6) NULL,

    -- 재발송 및 인증번호 입력 횟수
                                     send_count INT NOT NULL DEFAULT 1,
                                     attempt_count INT NOT NULL DEFAULT 0,

                                     last_sent_at DATETIME(6) NOT NULL,

                                     created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                     updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                     CONSTRAINT uk_email_verifications_email
                                         UNIQUE (email),

                                     CONSTRAINT chk_email_verification_send_count
                                         CHECK (send_count >= 0),

                                     CONSTRAINT chk_email_verification_attempt_count
                                         CHECK (attempt_count >= 0),

                                     INDEX idx_email_verification_expiry (
        code_expires_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- 3. TERMS_DOCUMENTS
-- 관리자가 작성하는 약관 및 버전
-- =====================================================

CREATE TABLE terms_documents (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- TERMS / PRIVACY_NOTICE / MARKETING
                                 terms_type VARCHAR(30) NOT NULL,

    -- 예: 1.0, 1.1, 2.0
                                 version VARCHAR(20) NOT NULL,

                                 title VARCHAR(200) NOT NULL,

                                 content LONGTEXT NOT NULL,

    -- 해당 버전에서 동의가 필수인지
                                 is_required BOOLEAN NOT NULL DEFAULT TRUE,

    -- DRAFT / PUBLISHED / ARCHIVED
                                 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- 시행 시각
                                 effective_at DATETIME(6) NULL,

    -- 게시 시각
                                 published_at DATETIME(6) NULL,

    -- 작성 관리자
                                 created_by BIGINT NULL,

                                 created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                 updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                 CONSTRAINT uk_terms_type_version
                                     UNIQUE (terms_type, version),

                                 CONSTRAINT fk_terms_created_by
                                     FOREIGN KEY (created_by)
                                         REFERENCES users(id),

                                 CONSTRAINT chk_terms_type
                                     CHECK (
                                         terms_type IN (
                                                        'TERMS',
                                                        'PRIVACY_NOTICE',
                                                        'MARKETING'
                                             )
                                         ),

                                 CONSTRAINT chk_terms_status
                                     CHECK (
                                         status IN (
                                                    'DRAFT',
                                                    'PUBLISHED',
                                                    'ARCHIVED'
                                             )
                                         ),

                                 INDEX idx_terms_published (
        terms_type,
        status,
        effective_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- 4. USER_TERMS_ACCEPTANCES
-- 어떤 회원이 어떤 약관 버전에 동의했는지 기록
-- =====================================================

CREATE TABLE user_terms_acceptances (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                        user_id BIGINT NOT NULL,
                                        terms_document_id BIGINT NOT NULL,

                                        accepted_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    -- LOCAL / KAKAO
                                        acceptance_source VARCHAR(20) NOT NULL,

                                        CONSTRAINT uk_user_terms_acceptance
                                            UNIQUE (user_id, terms_document_id),

                                        CONSTRAINT fk_terms_acceptance_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES users(id),

                                        CONSTRAINT fk_terms_acceptance_document
                                            FOREIGN KEY (terms_document_id)
                                                REFERENCES terms_documents(id),

                                        CONSTRAINT chk_terms_acceptance_source
                                            CHECK (
                                                acceptance_source IN (
                                                                      'LOCAL',
                                                                      'KAKAO'
                                                    )
                                                ),

                                        INDEX idx_terms_acceptance_document (
        terms_document_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

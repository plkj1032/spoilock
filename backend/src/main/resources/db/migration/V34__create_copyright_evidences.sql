CREATE TABLE copyright_evidences (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 어느 이용 허락 건의 증빙인지
                                     license_id BIGINT NOT NULL,

    -- 사용자가 첨부한 원본 파일명
                                     original_filename VARCHAR(255) NOT NULL,

    -- S3 내부 저장 경로. 공개 URL을 저장하지 않음
                                     storage_key VARCHAR(500)
                                            CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

    -- 서버에서 검증한 파일 형식과 크기
                                     content_type VARCHAR(100) NOT NULL,
                                     size_bytes BIGINT NOT NULL,

    -- 파일 내용의 SHA-256 해시
                                     sha256 CHAR(64)
                                            CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

    -- 첨부한 관리자
                                     uploaded_by BIGINT NOT NULL,

                                     created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                     CONSTRAINT fk_copyright_evidence_license
                                         FOREIGN KEY (license_id)
                                             REFERENCES copyright_licenses(id),

                                     CONSTRAINT fk_copyright_evidence_uploader
                                         FOREIGN KEY (uploaded_by)
                                             REFERENCES users(id),

                                     CONSTRAINT uk_copyright_evidence_storage
                                         UNIQUE (storage_key),

                                     CONSTRAINT chk_copyright_evidence_size
                                         CHECK (
                                             size_bytes > 0
                                                 AND size_bytes <= 10485760
                                             ),

                                     CONSTRAINT chk_copyright_evidence_type
                                         CHECK (
                                             content_type IN (
                                                              'application/pdf',
                                                              'image/jpeg',
                                                              'image/png'
                                                 )
                                             ),

                                     INDEX idx_copyright_evidence_license (
        license_id,
        created_at,
        id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
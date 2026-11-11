package com.spoilock.backend.copyright.domain.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "copyright_evidences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CopyrightEvidence extends CreatedAtEntity {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "license_id",
            nullable = false,
            updatable = false
    )
    private CopyrightLicense license;

    @Column(
            name = "original_filename",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String originalFilename;

    @Column(
            name = "storage_key",
            nullable = false,
            updatable = false,
            unique = true,
            length = 500
    )
    private String storageKey;

    @Column(
            name = "content_type",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String contentType;

    @Column(
            name = "size_bytes",
            nullable = false,
            updatable = false
    )
    private long sizeBytes;

    @Column(
            name = "sha256",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String sha256;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploaded_by",
            nullable = false,
            updatable = false
    )
    private User uploadedBy;

    public static CopyrightEvidence create(
            CopyrightLicense license,
            String originalFilename,
            String storageKey,
            String contentType,
            long sizeBytes,
            String sha256,
            User uploadedBy
    ) {
        if (license == null || uploadedBy == null) {
            throw new IllegalArgumentException(
                    "이용 권한과 첨부한 관리자 정보가 필요합니다."
            );
        }

        String filename = requiredText(
                originalFilename,
                255,
                "원본 파일명"
        );

        // 경로 또는 응답 헤더 제어문자가 파일명에 섞이지 않도록 검증
        if (filename.contains("/")
                || filename.contains("\\")
                || filename.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "사용할 수 없는 파일명입니다."
            );
        }

        String key = requiredText(
                storageKey,
                500,
                "파일 저장 경로"
        );

        if (key.codePoints().anyMatch(
                code -> code < 33 || code > 126
        )) {
            throw new IllegalArgumentException(
                    "파일 저장 경로 형식이 올바르지 않습니다."
            );
        }

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "PDF, JPG, PNG 파일만 첨부할 수 있습니다."
            );
        }

        if (sizeBytes <= 0 || sizeBytes > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "파일 크기는 0바이트 초과, 10MB 이하여야 합니다."
            );
        }

        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "파일 해시 형식이 올바르지 않습니다."
            );
        }

        CopyrightEvidence evidence = new CopyrightEvidence();

        evidence.license = license;
        evidence.originalFilename = filename;
        evidence.storageKey = key;
        evidence.contentType = contentType;
        evidence.sizeBytes = sizeBytes;
        evidence.sha256 = sha256;
        evidence.uploadedBy = uploadedBy;

        return evidence;
    }

    private static String requiredText(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }

        String trimmed = value.trim();

        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) "
                            + maxLength
                            + "자 이하여야 합니다."
            );
        }

        return trimmed;
    }
}
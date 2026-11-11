package com.spoilock.backend.copyright.domain.type;

public enum LicenseReviewStatus {
    DRAFT,       // 작성 중
    REVIEWING,   // 검토 중
    CONFIRMED,   // 확인 완료
    REVOKED      // 철회·해지
}

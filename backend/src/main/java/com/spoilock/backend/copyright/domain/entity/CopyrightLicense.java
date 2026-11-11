package com.spoilock.backend.copyright.domain.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.copyright.domain.type.LicenseReviewStatus;
import com.spoilock.backend.copyright.domain.type.UsagePermission;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "copyright_licenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CopyrightLicense extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대상 작품
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drama_id", nullable = false)
    private Drama drama;

    // 계약 또는 이용 허락 건 이름
    @Column(name = "license_name", nullable = false, length = 200)
    private String licenseName;

    // 권리자
    @Column(name = "rights_holder", nullable = false, length = 200)
    private String rightsHolder;

    // 연락 담당자
    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    // 관리자 확인 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private LicenseReviewStatus reviewStatus =
            LicenseReviewStatus.DRAFT;

    // 영상 제공 허락 여부
    @Enumerated(EnumType.STRING)
    @Column(name = "video_permission", nullable = false, length = 20)
    private UsagePermission videoPermission =
            UsagePermission.UNCONFIRMED;

    // 자막 사용 허락 여부
    @Enumerated(EnumType.STRING)
    @Column(name = "subtitle_permission", nullable = false, length = 20)
    private UsagePermission subtitlePermission =
            UsagePermission.UNCONFIRMED;

    // AI 분석 허락 여부
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_permission", nullable = false, length = 20)
    private UsagePermission aiPermission =
            UsagePermission.UNCONFIRMED;

    // 이용 기간
    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    // 대상 회차, 허용 지역 등 이용 범위
    @Column(name = "scope_notes", columnDefinition = "TEXT")
    private String scopeNotes;

    // 관리자 메모
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // 최초 등록 담당자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    // 마지막 수정 담당자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    // 여러 관리자가 동시에 수정할 때 충돌 감지
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    // =========================================
    // 1. 이용 권한 기록 생성
    // =========================================
    public static CopyrightLicense create(
            Drama drama,
            User actor,
            String licenseName,
            String rightsHolder,
            String contactName,
            String contactEmail,
            LicenseReviewStatus reviewStatus,
            UsagePermission videoPermission,
            UsagePermission subtitlePermission,
            UsagePermission aiPermission,
            LocalDate startsOn,
            LocalDate endsOn,
            String scopeNotes,
            String adminNotes
    ) {
        if (drama == null) {
            throw new IllegalArgumentException("대상 작품이 필요합니다.");
        }

        if (actor == null) {
            throw new IllegalArgumentException("등록 담당자가 필요합니다.");
        }

        CopyrightLicense license = new CopyrightLicense();

        license.drama = drama;
        license.createdBy = actor;

        license.update(
                actor,
                licenseName,
                rightsHolder,
                contactName,
                contactEmail,
                reviewStatus,
                videoPermission,
                subtitlePermission,
                aiPermission,
                startsOn,
                endsOn,
                scopeNotes,
                adminNotes
        );

        return license;
    }

    // =========================================
    // 2. 이용 권한 기록 수정
    // 작품과 최초 등록 담당자는 유지
    // =========================================
    public void update(
            User actor,
            String licenseName,
            String rightsHolder,
            String contactName,
            String contactEmail,
            LicenseReviewStatus reviewStatus,
            UsagePermission videoPermission,
            UsagePermission subtitlePermission,
            UsagePermission aiPermission,
            LocalDate startsOn,
            LocalDate endsOn,
            String scopeNotes,
            String adminNotes
    ) {
        if (actor == null) {
            throw new IllegalArgumentException("수정 담당자가 필요합니다.");
        }

        if (reviewStatus == null
                || videoPermission == null
                || subtitlePermission == null
                || aiPermission == null) {
            throw new IllegalArgumentException(
                    "확인 상태와 용도별 이용 허락 여부가 필요합니다."
            );
        }

        if (startsOn != null
                && endsOn != null
                && endsOn.isBefore(startsOn)) {
            throw new IllegalArgumentException(
                    "종료일은 시작일보다 빠를 수 없습니다."
            );
        }

        // 검증을 먼저 마친 후 필드에 반영
        String checkedName = requiredText(
                licenseName, 200, "이용 허락 건 이름"
        );
        String checkedHolder = requiredText(
                rightsHolder, 200, "권리자"
        );
        String checkedContactName = optionalText(
                contactName, 100, "연락 담당자"
        );
        String checkedContactEmail = optionalText(
                contactEmail, 255, "연락 이메일"
        );
        String checkedScopeNotes = optionalText(
                scopeNotes, 5000, "이용 범위"
        );
        String checkedAdminNotes = optionalText(
                adminNotes, 5000, "관리자 메모"
        );

        this.licenseName = checkedName;
        this.rightsHolder = checkedHolder;
        this.contactName = checkedContactName;
        this.contactEmail = checkedContactEmail;

        this.reviewStatus = reviewStatus;
        this.videoPermission = videoPermission;
        this.subtitlePermission = subtitlePermission;
        this.aiPermission = aiPermission;

        this.startsOn = startsOn;
        this.endsOn = endsOn;

        this.scopeNotes = checkedScopeNotes;
        this.adminNotes = checkedAdminNotes;
        this.updatedBy = actor;
    }

    // =========================================
    // 3. 필수 문자열 검증
    // =========================================
    private static String requiredText(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "을(를) 입력해 주세요."
            );
        }

        String trimmed = value.trim();

        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) "
                            + maxLength + "자 이하여야 합니다."
            );
        }

        return trimmed;
    }

    // =========================================
    // 4. 선택 문자열 정리 및 길이 검증
    // =========================================
    private static String optionalText(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) "
                            + maxLength + "자 이하여야 합니다."
            );
        }

        return trimmed;
    }
}
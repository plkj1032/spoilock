package com.spoilock.backend.copyright.domain.dto;

import com.spoilock.backend.copyright.domain.type.LicenseReviewStatus;
import com.spoilock.backend.copyright.domain.type.UsagePermission;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCopyrightLicenseRequest(

        @NotNull(message = "작품을 선택해 주세요.")
        @Positive(message = "올바른 작품 ID가 필요합니다.")
        Long dramaId,

        @NotBlank(message = "이용 허락 건 이름을 입력해 주세요.")
        @Size(max = 200, message = "이용 허락 건 이름은 200자 이하여야 합니다.")
        String licenseName,

        @NotBlank(message = "권리자를 입력해 주세요.")
        @Size(max = 200, message = "권리자는 200자 이하여야 합니다.")
        String rightsHolder,

        @Size(max = 100, message = "연락 담당자는 100자 이하여야 합니다.")
        String contactName,

        @Email(message = "올바른 이메일 주소를 입력해 주세요.")
        @Size(max = 255, message = "연락 이메일은 255자 이하여야 합니다.")
        String contactEmail,

        @NotNull(message = "확인 상태를 선택해 주세요.")
        LicenseReviewStatus reviewStatus,

        @NotNull(message = "영상 제공 허락 여부를 선택해 주세요.")
        UsagePermission videoPermission,

        @NotNull(message = "자막 사용 허락 여부를 선택해 주세요.")
        UsagePermission subtitlePermission,

        @NotNull(message = "AI 분석 허락 여부를 선택해 주세요.")
        UsagePermission aiPermission,

        LocalDate startsOn,

        LocalDate endsOn,

        @Size(max = 5000, message = "이용 범위는 5000자 이하여야 합니다.")
        String scopeNotes,

        @Size(max = 5000, message = "관리자 메모는 5000자 이하여야 합니다.")
        String adminNotes
) {

    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isDateRangeValid() {
        return startsOn == null
                || endsOn == null
                || !endsOn.isBefore(startsOn);
    }
}
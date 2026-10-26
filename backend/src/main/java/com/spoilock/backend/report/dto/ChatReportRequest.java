package com.spoilock.backend.report.dto;

import com.spoilock.backend.report.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatReportRequest(
        @NotNull(message = "신고 사유를 선택해 주세요")
        ReportReason reason,

        @Size(max=500, message = "상세설명은 500자 이하로 입력해 주세요.")
        String detail
) {
}

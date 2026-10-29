package com.spoilock.backend.spoiler.dto;

import jakarta.validation.constraints.Size;

/** 사건 내용 수정. 보낸 항목만 바뀐다 (null 은 그대로 유지) */
public record UpdateSpoilerEventRequest(
        @Size(min = 1, max = 500)
        String eventSummary,

        String eventType,

        String factStatus
) {
}

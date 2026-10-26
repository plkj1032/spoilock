package com.spoilock.backend.report.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {

    SPOILER("스포일러"),   // 스포일러 신고
    SPAM("광고 및 도배"),      // 광고 및 도배
    ABUSE("욕설 및 비방"),     // 욕설 및 비방
    OTHER("기타")   ;   // 기타

    private final String label;
}

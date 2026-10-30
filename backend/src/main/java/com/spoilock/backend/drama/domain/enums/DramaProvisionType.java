package com.spoilock.backend.drama.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DramaProvisionType {

    INFO_ONLY("정보만 제공"),
    STREAMING("영상 제공");

    private final String label;
}
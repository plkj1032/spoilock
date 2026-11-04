package com.spoilock.backend.ai.service;

/**
 * 보류 판정 공통 상수 (Spring 의존성 없음 → 단위 테스트 가능).
 */
public final class HoldRules {

    /** 보류를 당분간 전체 공개한 결정의 사유 앞부분 (모니터링에서 '보류'로 분류) */
    public static final String HELD_PUBLISHED_PREFIX = "보류 공개: ";

    private HoldRules() {
    }
}
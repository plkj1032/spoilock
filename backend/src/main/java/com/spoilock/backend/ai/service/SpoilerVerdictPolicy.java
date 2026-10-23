package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerJudgeResponse;

import java.util.List;

/**
 * Python 판정(verdict) → 공개 정책 결정.
 * 원칙: 확실히 공개해도 되는 경우만 공개하고, 나머지는 보류(fail-closed).
 * Spring 의존성이 없는 순수 클래스라 단위 테스트가 쉽다.
 */
public final class SpoilerVerdictPolicy {

    public static final String UNVERIFIED_BASIS_PREFIX = "UNVERIFIED";

    private final boolean allowUnverifiedDemo;
    private final boolean publishSkippedGeneral;
    private final boolean publishSkippedUnrelated;
    private final boolean publishNoMatch;

    public SpoilerVerdictPolicy(
            boolean allowUnverifiedDemo,
            boolean publishSkippedGeneral,
            boolean publishSkippedUnrelated,
            boolean publishNoMatch
    ) {
        this.allowUnverifiedDemo = allowUnverifiedDemo;
        this.publishSkippedGeneral = publishSkippedGeneral;
        this.publishSkippedUnrelated = publishSkippedUnrelated;
        this.publishNoMatch = publishNoMatch;
    }

    public AnalysisDecision decide(
            SpoilerJudgeResponse response,
            List<Integer> indexedEpisodes
    ) {
        if (response == null || response.verdict() == null) {
            return AnalysisDecision.failed("빈 판정");
        }

        return switch (response.verdict()) {

            case "MATCH" -> decideMatch(response, indexedEpisodes);

            case "SKIPPED_GENERAL" -> publishSkippedGeneral
                    ? AnalysisDecision.publishAll("1차 필터: 일반 대화")
                    : AnalysisDecision.hold("1차 필터: 일반 대화(공개 설정 꺼짐)");

            case "SKIPPED_UNRELATED" -> publishSkippedUnrelated
                    ? AnalysisDecision.publishAll("1차 필터: 사건과 무관")
                    : AnalysisDecision.hold("1차 필터: 사건과 무관(공개 설정 꺼짐)");

            // 등록된 사건과 맞지 않음 ≠ 스포일러 아님. 기본은 보류
            case "NO_MATCH" -> publishNoMatch
                    ? AnalysisDecision.publishAll("NO_MATCH 공개 정책")
                    : AnalysisDecision.hold("NO_MATCH: 검토 대기");

            case "UNCERTAIN" -> AnalysisDecision.hold("UNCERTAIN: 검토 대기");

            // Python 쪽 Gemini 오류·시간 초과 → 재시도 대상
            case "ERROR" -> AnalysisDecision.failed(
                    "분석 서버 오류: " + nullToEmpty(response.error())
            );

            default -> AnalysisDecision.failed("알 수 없는 판정: " + response.verdict());
        };
    }

    private AnalysisDecision decideMatch(
            SpoilerJudgeResponse response,
            List<Integer> indexedEpisodes
    ) {
        Integer episode = response.requiredEpisodeNumber();

        if (response.matchedEventId() == null || response.matchedEventId().isBlank()) {
            return AnalysisDecision.hold("MATCH 인데 사건 ID 없음");
        }

        if (episode == null || episode <= 0) {
            return AnalysisDecision.hold("MATCH 인데 회차 없음");
        }

        if (indexedEpisodes == null || !indexedEpisodes.contains(episode)) {
            return AnalysisDecision.hold("MATCH 회차가 색인 범위 밖: " + episode);
        }

        if (Boolean.TRUE.equals(response.needsReview())) {
            return AnalysisDecision.hold("MATCH 이지만 검토 필요");
        }

        boolean unverified = response.episodeBasis() == null
                || response.episodeBasis().startsWith(UNVERIFIED_BASIS_PREFIX);

        if (unverified && !allowUnverifiedDemo) {
            return AnalysisDecision.hold("미검증 사건 회차(데모 허용 꺼짐)");
        }

        return AnalysisDecision.publishFrom(
                episode,
                "MATCH " + response.matchedEventId() + " → " + episode + "화"
                        + (unverified ? " (미검증 데모)" : "")
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

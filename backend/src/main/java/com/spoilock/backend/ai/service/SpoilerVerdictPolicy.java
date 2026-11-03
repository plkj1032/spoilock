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
    private final boolean publishOffTopic;

    public SpoilerVerdictPolicy(
            boolean allowUnverifiedDemo,
            boolean publishSkippedGeneral,
            boolean publishSkippedUnrelated,
            boolean publishNoMatch,
            boolean publishOffTopic
    ) {
        this.allowUnverifiedDemo = allowUnverifiedDemo;
        this.publishSkippedGeneral = publishSkippedGeneral;
        this.publishSkippedUnrelated = publishSkippedUnrelated;
        this.publishNoMatch = publishNoMatch;
        this.publishOffTopic = publishOffTopic;
    }

    public AnalysisDecision decide(
            SpoilerJudgeResponse response,
            List<Integer> indexedEpisodes
    ) {
        return decide(response, indexedEpisodes, null);
    }

    /**
     * @param authorWatchedEpisode 작성자의 이 작품 시청 회차 (모르면 null, 안 봤으면 0).
     *                             추측성 글이 작성자가 이미 본 회차의 사건과 맞으면 그 회차부터 공개한다.
     */
    public AnalysisDecision decide(
            SpoilerJudgeResponse response,
            List<Integer> indexedEpisodes,
            Integer authorWatchedEpisode
    ) {
        AnalysisDecision decision = decideVerdict(response, indexedEpisodes);
        return applyAuthorGuess(decision, response, indexedEpisodes, authorWatchedEpisode);
    }

    private AnalysisDecision decideVerdict(
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

            // 등록된 사건과 맞지 않음 ≠ 스포일러 아님.
            // 단, Gemini가 '드라마 이야기가 아님'으로 판단한 일상 대화는 공개 (about_story=false 일 때만)
            case "NO_MATCH" -> decideNoMatch(response);

            // 사건과 맞는지 확신 못 함. 단, 드라마 이야기가 아니라고 판단했으면 공개 (about_story=false 일 때만)
            case "UNCERTAIN" -> decideUncertain(response);

            // Python 쪽 Gemini 오류·시간 초과 → 재시도 대상
            case "ERROR" -> AnalysisDecision.failed(
                    "분석 서버 오류: " + nullToEmpty(response.error())
            );

            default -> AnalysisDecision.failed("알 수 없는 판정: " + response.verdict());
        };
    }

    private AnalysisDecision decideNoMatch(SpoilerJudgeResponse response) {
        if (publishNoMatch) {
            return AnalysisDecision.publishAll("NO_MATCH 공개 정책");
        }
        if (publishOffTopic && Boolean.FALSE.equals(response.aboutStory())) {
            return AnalysisDecision.publishAll("NO_MATCH: 작품과 무관한 대화");
        }
        // about_story=true 또는 알 수 없음(null) → 등록되지 않은 사건일 수 있으므로 보류
        return AnalysisDecision.hold("NO_MATCH: 검토 대기");
    }

    private AnalysisDecision decideUncertain(SpoilerJudgeResponse response) {
        return AnalysisDecision.hold("UNCERTAIN: 검토 대기");
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

    /**
     * 추측·질문 형태지만 사건과 맞는 글.
     * 작성자가 아직 안 본 회차 → 모르고 하는 추측이므로 기존 결정 유지 (보통 전체 공개).
     * 작성자가 이미 본 회차   → 아는 내용을 추측처럼 말한 것이므로 그 회차부터 공개.
     */
    private AnalysisDecision applyAuthorGuess(
            AnalysisDecision decision,
            SpoilerJudgeResponse response,
            List<Integer> indexedEpisodes,
            Integer authorWatchedEpisode
    ) {
        if (decision.failed()
                || response == null
                || authorWatchedEpisode == null          // 작성자 진도 조회 실패 → 기존 결정 유지
                || response.guessEpisodes() == null
                || response.guessEpisodes().isEmpty()
                || indexedEpisodes == null) {
            return decision;
        }

        List<Integer> guessed = response.guessEpisodes().stream()
                .filter(e -> e != null && e > 0 && indexedEpisodes.contains(e))
                .toList();

        if (guessed.isEmpty()) {
            return decision;
        }

        int author = Math.max(0, authorWatchedEpisode);

        Integer known = guessed.stream()
                .filter(e -> e <= author)
                .max(Integer::compareTo)
                .orElse(null);

        if (known == null) {
            // 작성자가 아직 안 본 회차에 대한 추측 = 모르고 하는 추측 → 전체 공개.
            // (Gemini 가 NO_MATCH·UNCERTAIN 어느 쪽으로 판정했든, 공개 정책 설정과 무관하게 같은 결과·같은 사유)
            // MATCH 는 같은 글에 사실 진술이 섞인 경우라 그대로 둔다.
            if ("MATCH".equals(response.verdict())) {
                return decision;
            }
            return AnalysisDecision.publishAll(
                    "안 본 회차 추측: 작성자 " + author + "화, 추측 내용 "
                            + guessed.stream().min(Integer::compareTo).orElse(0) + "화 이후"
            );
        }

        boolean unverified = response.guessBasis() == null
                || response.guessBasis().startsWith(UNVERIFIED_BASIS_PREFIX);

        if (unverified && !allowUnverifiedDemo) {
            return decision.publish()
                    ? AnalysisDecision.hold("작성자가 본 회차의 추측이지만 미검증 사건(데모 허용 꺼짐)")
                    : decision;
        }

        String note = "추측이지만 작성자가 이미 본 " + known + "화 내용 (작성자 " + authorWatchedEpisode + "화)";

        if (!decision.publish()) {
            // NO_MATCH 보류는 추측 회차로 공개 범위를 정할 수 있음. UNCERTAIN 등 다른 보류는 유지
            return "NO_MATCH".equals(response.verdict())
                    ? AnalysisDecision.publishFrom(known, note)
                    : decision;
        }

        Integer current = decision.requiredEpisode();
        if (current != null && current >= known) {
            return decision;
        }
        return AnalysisDecision.publishFrom(
                known,
                current == null ? note : note + " / " + decision.note()
        );
    }

    /**
     * 보류(검토 대기) 결정을 '드라마 마지막 회차부터 공개'로 바꾼다.
     * 보류는 관리자가 처리하기 전까지 작성자 외 누구에게도 보이지 않아 사실상 영구 비공개였다.
     * 끝까지 본 사람에게만 보이므로 스포일러는 막으면서 글이 사라지지 않는다.
     *
     * @param finaleEpisode 작품의 마지막 회차 (전체 회차 수·등록 회차 중 큰 값, 모르면 null)
     */
    public static AnalysisDecision toFinale(
            AnalysisDecision decision,
            Integer finaleEpisode,
            List<Integer> indexedEpisodes
    ) {
        if (decision == null || decision.failed() || decision.publish()) {
            return decision;
        }

        int finale = finaleEpisode == null ? 0 : finaleEpisode;
        if (indexedEpisodes != null) {
            for (Integer ep : indexedEpisodes) {
                if (ep != null && ep > finale) {
                    finale = ep;
                }
            }
        }

        if (finale <= 0) {
            return decision;   // 마지막 회차를 알 수 없으면 보류 유지
        }

        return AnalysisDecision.publishUntilFinale(
                finale,
                "마지막 회차 기준(" + finale + "화): " + decision.note()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
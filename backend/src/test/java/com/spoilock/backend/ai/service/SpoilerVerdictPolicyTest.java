package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerJudgeResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 판정 → 공개 정책 단위 테스트 (Spring 컨텍스트 불필요)
 */
class SpoilerVerdictPolicyTest {

    private static final List<Integer> EPISODES = List.of(1, 2, 3, 4, 5, 6);
    private static final String DEMO_BASIS = "UNVERIFIED_SOURCE_EPISODE_FOR_DEMO_ONLY";

    // 데모 허용, 일반 대화 공개, 나머지 보수적
    private final SpoilerVerdictPolicy demoPolicy =
            new SpoilerVerdictPolicy(true, true, false, false, true);

    // 운영 기본값: 미검증 회차 불허
    private final SpoilerVerdictPolicy strictPolicy =
            new SpoilerVerdictPolicy(false, true, false, false, true);

    private static SpoilerJudgeResponse response(
            String verdict, String eventId, Integer episode, String basis, Boolean needsReview
    ) {
        return response(verdict, eventId, episode, basis, needsReview, null);
    }

    private static SpoilerJudgeResponse response(
            String verdict, String eventId, Integer episode, String basis, Boolean needsReview, Boolean aboutStory
    ) {
        return new SpoilerJudgeResponse(1L, verdict, eventId, episode, basis, needsReview, "r", null, aboutStory);
    }

    @Test
    void match_with_demo_allowed_publishes_from_episode() {
        AnalysisDecision d = demoPolicy.decide(
                response("MATCH", "d1-e104-kb1-f0009", 4, DEMO_BASIS, false), EPISODES);

        assertFalse(d.failed());
        assertTrue(d.publish());
        assertEquals(4, d.requiredEpisode());
    }

    @Test
    void match_unverified_is_held_when_demo_not_allowed() {
        AnalysisDecision d = strictPolicy.decide(
                response("MATCH", "d1-e104-kb1-f0009", 4, DEMO_BASIS, false), EPISODES);

        assertFalse(d.failed());
        assertFalse(d.publish());
        assertNull(d.requiredEpisode());
    }

    @Test
    void match_verified_is_published_even_in_strict_mode() {
        AnalysisDecision d = strictPolicy.decide(
                response("MATCH", "e-1", 3, "VERIFIED_FIRST_REVEAL", false), EPISODES);

        assertTrue(d.publish());
        assertEquals(3, d.requiredEpisode());
    }

    @Test
    void match_with_invalid_data_is_held() {
        assertFalse(demoPolicy.decide(response("MATCH", null, 4, DEMO_BASIS, false), EPISODES).publish());
        assertFalse(demoPolicy.decide(response("MATCH", " ", 4, DEMO_BASIS, false), EPISODES).publish());
        assertFalse(demoPolicy.decide(response("MATCH", "e", null, DEMO_BASIS, false), EPISODES).publish());
        assertFalse(demoPolicy.decide(response("MATCH", "e", 0, DEMO_BASIS, false), EPISODES).publish());
        assertFalse(demoPolicy.decide(response("MATCH", "e", 9, DEMO_BASIS, false), EPISODES).publish());
        assertFalse(demoPolicy.decide(response("MATCH", "e", 4, DEMO_BASIS, true), EPISODES).publish());
        assertFalse(demoPolicy.decide(response("MATCH", "e", 4, DEMO_BASIS, false), null).publish());
    }

    @Test
    void skipped_general_is_public_without_episode() {
        AnalysisDecision d = demoPolicy.decide(
                response("SKIPPED_GENERAL", null, null, DEMO_BASIS, false), EPISODES);

        assertTrue(d.publish());
        assertNull(d.requiredEpisode());
    }

    @Test
    void no_match_uncertain_and_unrelated_are_held_by_default() {
        for (String verdict : List.of("NO_MATCH", "UNCERTAIN", "SKIPPED_UNRELATED")) {
            AnalysisDecision d = demoPolicy.decide(
                    response(verdict, null, null, DEMO_BASIS, true), EPISODES);

            assertFalse(d.failed(), verdict);
            assertFalse(d.publish(), verdict);
        }
    }

    @Test
    void no_match_can_be_published_by_policy() {
        SpoilerVerdictPolicy openNoMatch = new SpoilerVerdictPolicy(true, true, false, true, true);

        AnalysisDecision d = openNoMatch.decide(
                response("NO_MATCH", null, null, DEMO_BASIS, true), EPISODES);

        assertTrue(d.publish());
        assertNull(d.requiredEpisode());
    }

    @Test
    void no_match_off_topic_is_published() {
        AnalysisDecision d = demoPolicy.decide(
                response("NO_MATCH", null, null, DEMO_BASIS, true, false), EPISODES);

        assertTrue(d.publish());
        assertNull(d.requiredEpisode());
    }

    @Test
    void no_match_about_story_or_unknown_is_held() {
        assertFalse(demoPolicy.decide(
                response("NO_MATCH", null, null, DEMO_BASIS, true, true), EPISODES).publish());
        assertFalse(demoPolicy.decide(
                response("NO_MATCH", null, null, DEMO_BASIS, true, null), EPISODES).publish());
    }

    @Test
    void no_match_off_topic_is_held_when_disabled() {
        SpoilerVerdictPolicy closed = new SpoilerVerdictPolicy(true, true, false, false, false);

        assertFalse(closed.decide(
                response("NO_MATCH", null, null, DEMO_BASIS, true, false), EPISODES).publish());
    }

    @Test
    void uncertain_is_held_even_if_off_topic() {
        assertFalse(demoPolicy.decide(
                response("UNCERTAIN", null, null, DEMO_BASIS, true, false), EPISODES).publish());
    }

    @Test
    void error_and_unknown_verdicts_fail() {
        assertTrue(demoPolicy.decide(response("ERROR", null, null, DEMO_BASIS, true), EPISODES).failed());
        assertTrue(demoPolicy.decide(response("SOMETHING_NEW", null, null, DEMO_BASIS, true), EPISODES).failed());
        assertTrue(demoPolicy.decide(null, EPISODES).failed());
        assertTrue(demoPolicy.decide(response(null, null, null, null, null), EPISODES).failed());
    }

    // ---------------------------------------------------------------- 추측성 글 + 작성자 시청 진도

    private static SpoilerJudgeResponse guess(String verdict, Integer matchEpisode, String basis,
                                              Boolean aboutStory, List<Integer> guessEpisodes, String guessBasis) {
        return new SpoilerJudgeResponse(1L, verdict, matchEpisode == null ? null : "e-m", matchEpisode,
                basis, matchEpisode == null, "r", null, aboutStory,
                List.of("e-g"), guessEpisodes, guessBasis);
    }

    @Test
    void guess_by_author_who_has_not_watched_is_public() {
        AnalysisDecision d = demoPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, false, List.of(3), "VERIFIED_FIRST_REVEAL"), EPISODES, 1);

        assertTrue(d.publish());
        assertNull(d.requiredEpisode());
    }

    @Test
    void guess_by_author_who_already_watched_is_hidden_until_that_episode() {
        AnalysisDecision d = demoPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, false, List.of(2), "VERIFIED_FIRST_REVEAL"), EPISODES, 3);

        assertTrue(d.publish());
        assertEquals(2, d.requiredEpisode());
    }

    @Test
    void guess_uses_latest_episode_the_author_has_watched() {
        AnalysisDecision d = demoPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, false, List.of(2, 4, 6), "VERIFIED_FIRST_REVEAL"), EPISODES, 5);

        assertEquals(4, d.requiredEpisode());
    }

    @Test
    void guess_raises_episode_of_match_when_later() {
        AnalysisDecision raised = demoPolicy.decide(
                guess("MATCH", 2, "VERIFIED_FIRST_REVEAL", true, List.of(4), "VERIFIED_FIRST_REVEAL"), EPISODES, 6);
        assertEquals(4, raised.requiredEpisode());

        AnalysisDecision kept = demoPolicy.decide(
                guess("MATCH", 5, "VERIFIED_FIRST_REVEAL", true, List.of(4), "VERIFIED_FIRST_REVEAL"), EPISODES, 6);
        assertEquals(5, kept.requiredEpisode());
    }

    @Test
    void held_no_match_becomes_episode_bound_when_author_watched_guess() {
        // about_story=true NO_MATCH 는 원래 보류 → 작성자가 본 회차의 추측이면 그 회차부터 공개
        AnalysisDecision d = demoPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, true, List.of(3), "VERIFIED_FIRST_REVEAL"), EPISODES, 4);

        assertTrue(d.publish());
        assertEquals(3, d.requiredEpisode());
    }

    @Test
    void guess_is_ignored_when_progress_unknown_or_episode_out_of_range() {
        assertNull(demoPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, false, List.of(3), "VERIFIED_FIRST_REVEAL"), EPISODES, null)
                .requiredEpisode());
        assertNull(demoPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, false, List.of(3), "VERIFIED_FIRST_REVEAL"), EPISODES, 0)
                .requiredEpisode());
        assertNull(demoPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, false, List.of(9), "VERIFIED_FIRST_REVEAL"), EPISODES, 9)
                .requiredEpisode());
    }

    @Test
    void unverified_guess_is_held_in_strict_mode() {
        AnalysisDecision d = strictPolicy.decide(
                guess("NO_MATCH", null, DEMO_BASIS, false, List.of(2), DEMO_BASIS), EPISODES, 3);

        assertFalse(d.failed());
        assertFalse(d.publish());
    }

    @Test
    void uncertain_stays_held_even_with_known_guess() {
        AnalysisDecision d = demoPolicy.decide(
                guess("UNCERTAIN", null, DEMO_BASIS, true, List.of(2), "VERIFIED_FIRST_REVEAL"), EPISODES, 3);

        assertFalse(d.publish());
    }

    // ---------------------------------------------------------------- 보류 → 마지막 회차부터 공개

    @Test
    void held_decision_becomes_visible_from_finale() {
        AnalysisDecision held = demoPolicy.decide(
                response("UNCERTAIN", null, null, DEMO_BASIS, true), EPISODES);
        assertFalse(held.publish());

        AnalysisDecision d = SpoilerVerdictPolicy.toFinale(held, 16, EPISODES);

        assertTrue(d.publish());
        assertTrue(d.untilFinale());
        assertEquals(16, d.requiredEpisode());
        assertEquals(AnalysisPolicyCode.FINALE_ONLY, AnalysisPolicyCode.from(d));
    }

    @Test
    void finale_is_at_least_last_indexed_episode() {
        AnalysisDecision held = AnalysisDecision.hold("NO_MATCH: 검토 대기");

        assertEquals(6, SpoilerVerdictPolicy.toFinale(held, 4, EPISODES).requiredEpisode());
        assertEquals(6, SpoilerVerdictPolicy.toFinale(held, null, EPISODES).requiredEpisode());
    }

    @Test
    void finale_keeps_published_failed_and_unknown_finale() {
        AnalysisDecision published = AnalysisDecision.publishFrom(3, "MATCH");
        AnalysisDecision failed = AnalysisDecision.failed("오류");
        AnalysisDecision held = AnalysisDecision.hold("UNCERTAIN: 검토 대기");

        assertEquals(published, SpoilerVerdictPolicy.toFinale(published, 16, EPISODES));
        assertEquals(failed, SpoilerVerdictPolicy.toFinale(failed, 16, EPISODES));
        assertEquals(held, SpoilerVerdictPolicy.toFinale(held, null, List.of()));
        assertFalse(SpoilerVerdictPolicy.toFinale(held, null, null).publish());
    }
}
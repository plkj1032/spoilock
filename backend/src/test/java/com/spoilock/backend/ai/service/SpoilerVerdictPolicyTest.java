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
            new SpoilerVerdictPolicy(true, true, false, false);

    // 운영 기본값: 미검증 회차 불허
    private final SpoilerVerdictPolicy strictPolicy =
            new SpoilerVerdictPolicy(false, true, false, false);

    private static SpoilerJudgeResponse response(
            String verdict, String eventId, Integer episode, String basis, Boolean needsReview
    ) {
        return new SpoilerJudgeResponse(1L, verdict, eventId, episode, basis, needsReview, "r", null);
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
        SpoilerVerdictPolicy openNoMatch = new SpoilerVerdictPolicy(true, true, false, true);

        AnalysisDecision d = openNoMatch.decide(
                response("NO_MATCH", null, null, DEMO_BASIS, true), EPISODES);

        assertTrue(d.publish());
        assertNull(d.requiredEpisode());
    }

    @Test
    void error_and_unknown_verdicts_fail() {
        assertTrue(demoPolicy.decide(response("ERROR", null, null, DEMO_BASIS, true), EPISODES).failed());
        assertTrue(demoPolicy.decide(response("SOMETHING_NEW", null, null, DEMO_BASIS, true), EPISODES).failed());
        assertTrue(demoPolicy.decide(null, EPISODES).failed());
        assertTrue(demoPolicy.decide(response(null, null, null, null, null), EPISODES).failed());
    }
}

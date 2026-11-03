package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.ImageJudgeResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 이미지 판정 → 블러 기준 회차 단위 테스트 (Spring 컨텍스트 불필요)
 */
class ImageVerdictPolicyTest {

    private static final List<Integer> EPISODES = List.of(1, 2, 3, 4, 5, 6);

    private static ImageJudgeResponse response(String verdict, Integer episode) {
        return new ImageJudgeResponse(1L, verdict, episode, List.of(), null, "r", null);
    }

    @Test
    void match_blurs_until_episode() {
        ImageVerdictPolicy.ImageDecision d =
                ImageVerdictPolicy.decide(response("MATCH", 3), EPISODES);

        assertFalse(d.failed());
        assertEquals(3, d.requiredEpisode());
    }

    @Test
    void match_without_episode_and_uncertain_blur_until_last_episode() {
        assertEquals(6, ImageVerdictPolicy.decide(response("MATCH", null), EPISODES).requiredEpisode());
        assertEquals(6, ImageVerdictPolicy.decide(response("UNCERTAIN", null), EPISODES).requiredEpisode());
    }

    @Test
    void no_match_and_skipped_show_to_everyone() {
        for (String verdict : List.of("NO_MATCH", "SKIPPED_NO_VIDEO_EVENTS", "SKIPPED_NOT_SCENE")) {
            ImageVerdictPolicy.ImageDecision d = ImageVerdictPolicy.decide(response(verdict, null), EPISODES);
            assertFalse(d.failed(), verdict);
            assertNull(d.requiredEpisode(), verdict);
        }
    }

    @Test
    void errors_fail_so_image_stays_blurred_and_is_retried() {
        assertTrue(ImageVerdictPolicy.decide(response("ERROR", null), EPISODES).failed());
        assertTrue(ImageVerdictPolicy.decide(response("SOMETHING_NEW", null), EPISODES).failed());
        assertTrue(ImageVerdictPolicy.decide(response(null, null), EPISODES).failed());
        assertTrue(ImageVerdictPolicy.decide(null, EPISODES).failed());
        assertTrue(ImageVerdictPolicy.decide(response("UNCERTAIN", null), List.of()).failed());
    }
}

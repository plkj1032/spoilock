package com.spoilock.backend.post.entity;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 게시글 스포일러 분석 결과 반영 규칙 (Spring 컨텍스트·DB 불필요)
 */
class PostAnalysisStateTest {

    private static Post newPost() {
        return Post.create(null, null, " 내용 ");
    }

    @Test
    void publishAll_전원공개() {
        Post post = newPost();
        post.completeAnalysis(true, null);

        assertEquals(AnalysisStatus.COMPLETED, post.getAnalysisStatus());
        assertEquals(ModerationStatus.VISIBLE, post.getModerationStatus());
        assertNull(post.getRequiredEpisodeNumber());
    }

    @Test
    void publishFrom_회차이상공개() {
        Post post = newPost();
        post.completeAnalysis(true, 3);

        assertEquals(ModerationStatus.VISIBLE, post.getModerationStatus());
        assertEquals(3, post.getRequiredEpisodeNumber());
    }

    @Test
    void hold_검토대기_회차무시() {
        Post post = newPost();
        post.completeAnalysis(false, 3);

        assertEquals(AnalysisStatus.COMPLETED, post.getAnalysisStatus());
        assertEquals(ModerationStatus.PENDING, post.getModerationStatus());
        assertNull(post.getRequiredEpisodeNumber());
    }

    @Test
    void failed_원문차단_재시도대상() {
        Post post = newPost();
        post.completeAnalysis(true, 2);
        post.failAnalysis();

        assertEquals(AnalysisStatus.FAILED, post.getAnalysisStatus());
        assertEquals(ModerationStatus.PENDING, post.getModerationStatus());
        assertNull(post.getRequiredEpisodeNumber());
    }
}

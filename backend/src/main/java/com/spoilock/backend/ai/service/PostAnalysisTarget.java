package com.spoilock.backend.ai.service;

import com.spoilock.backend.common.enums.AnalysisStatus;

/**
 * 복구 작업이 다시 분석할 게시글·댓글 (JPQL constructor projection).
 */
public record PostAnalysisTarget(
        Long postId,
        Long dramaId,
        String content,
        AnalysisStatus analysisStatus
) {

    public PostAnalysisRequested toEvent() {
        return new PostAnalysisRequested(postId, dramaId, content);
    }
}

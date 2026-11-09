
package com.spoilock.backend.ai.service;

import com.spoilock.backend.common.enums.AnalysisStatus;

public record PostAnalysisTarget(
        Long postId,
        Long dramaId,
        String content,
        AnalysisStatus analysisStatus,
        long revision
) {

    public PostAnalysisRequested toEvent() {
        return new PostAnalysisRequested(
                postId,
                dramaId,
                content,
                revision
        );
    }
}

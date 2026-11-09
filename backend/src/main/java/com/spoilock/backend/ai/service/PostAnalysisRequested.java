
package com.spoilock.backend.ai.service;

public record PostAnalysisRequested(
        Long postId,
        Long dramaId,
        String content,
        long revision
) {
}

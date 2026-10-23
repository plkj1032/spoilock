package com.spoilock.backend.ai.service;

import com.spoilock.backend.common.enums.AnalysisStatus;

/**
 * 복구 작업이 다시 분석할 메시지 (JPQL constructor projection).
 */
public record ChatAnalysisTarget(
        Long messageId,
        Long roomId,
        Long dramaId,
        String content,
        AnalysisStatus analysisStatus
) {

    public ChatAnalysisRequested toEvent() {
        return new ChatAnalysisRequested(messageId, roomId, dramaId, content);
    }
}

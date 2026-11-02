package com.spoilock.backend.ai.service;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 이미지 업로드 트랜잭션이 커밋된 뒤 별도 스레드에서 판정 시작 (업로드 API 는 기다리지 않음).
 */
@Component
@RequiredArgsConstructor
public class MediaAnalysisListener {

    private final MediaAnalysisService mediaAnalysisService;

    @Async(ChatAnalysisAsyncConfig.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MediaAnalysisRequested event) {
        for (Long mediaId : event.mediaIds()) {
            mediaAnalysisService.analyze(mediaId);
        }
    }
}

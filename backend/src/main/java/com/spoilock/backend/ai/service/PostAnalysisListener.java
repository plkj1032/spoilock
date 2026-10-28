package com.spoilock.backend.ai.service;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시글·댓글 저장 트랜잭션이 커밋된 뒤에만 분석 시작 (롤백된 글은 분석하지 않음).
 * 별도 스레드(spoilerExecutor, 채팅과 공유)에서 실행 → 작성 API 가 Gemini 응답을 기다리지 않음.
 */
@Component
@RequiredArgsConstructor
public class PostAnalysisListener {

    private final PostAnalysisService postAnalysisService;

    @Async(ChatAnalysisAsyncConfig.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostAnalysisRequested event) {
        postAnalysisService.analyze(event);
    }
}

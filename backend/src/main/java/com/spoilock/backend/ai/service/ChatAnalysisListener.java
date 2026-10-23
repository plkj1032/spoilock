package com.spoilock.backend.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 메시지 저장 트랜잭션이 커밋된 뒤에만 분석 시작 (롤백된 메시지는 분석하지 않음).
 * 별도 스레드(spoilerExecutor)에서 실행 → WebSocket 전송 처리가 Gemini 응답을 기다리지 않음.
 *
 * 주의: Redis 구독(ChatRedisSubscriber)에서 분석을 시작하지 말 것.
 *       Spring 서버가 여러 대면 같은 메시지를 중복 분석하게 된다.
 */
@Component
@RequiredArgsConstructor
public class ChatAnalysisListener {

    private final ChatAnalysisService chatAnalysisService;

    @Async(ChatAnalysisAsyncConfig.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatAnalysisRequested event) {
        chatAnalysisService.analyze(event);
    }
}

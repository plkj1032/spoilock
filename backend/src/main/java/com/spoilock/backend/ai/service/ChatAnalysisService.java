
package com.spoilock.backend.ai.service;

import com.spoilock.backend.admin.monitoring.MonitoringTraceService;
import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 채팅 AI 스포일러 분석 서비스
 *
 * 1. 분석 작업 선점
 * 2. 작성자 시청 진도 조회 및 AI 판정
 * 3. AI 분석 처리시간 측정
 * 4. 분석 결과 및 모니터링 데이터 DB 저장
 * 5. Redis 갱신 알림 전송
 * 6. 각 단계별 관리자 모니터링 기록
 *
 * AI HTTP 요청 중 DB 트랜잭션을 유지하지 않기 위해
 * 클래스 전체에 @Transactional을 사용하지 않는다.
 */
@Slf4j
@Service
public class ChatAnalysisService {

    private final ChatAnalysisStateService stateService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final SpoilerJudgeService judgeService;

    // 작성자의 드라마 시청 진도 조회
    private final AuthorProgressService authorProgressService;

    // 관리자 모니터링 단계별 기록
    private final MonitoringTraceService traceService;

    public ChatAnalysisService(
            ChatAnalysisStateService stateService,
            ChatRedisPublisher chatRedisPublisher,
            SpoilerJudgeService judgeService,
            AuthorProgressService authorProgressService,
            MonitoringTraceService traceService
    ) {
        this.stateService = stateService;
        this.chatRedisPublisher = chatRedisPublisher;
        this.judgeService = judgeService;
        this.authorProgressService = authorProgressService;
        this.traceService = traceService;
    }

    /**
     * 채팅 메시지 AI 분석 전체 처리 흐름
     */
    public void analyze(ChatAnalysisRequested event) {

        Long messageId = event.messageId();

        // ========================================
        // 1. 분석 작업 선점
        // ========================================
        if (!stateService.tryStart(messageId)) {
            // 이미 분석 중이거나 완료된 메시지
            return;
        }

        // ========================================
        // 2. AI 분석 시작 시간 기록
        // ========================================
        long startedAt = System.nanoTime();

        // ========================================
        // 3. 작성자 시청 진도 + AI 스포일러 판정
        // ========================================
        AnalysisDecision decision = judge(event);

        // AI 판정 처리시간(ms)
        long durationMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );

        // ========================================
        // 4. AI 판정 단계 기록
        // ========================================
        traceService.record(
                messageId,
                decision.failed() ? "AI_FAILED" : "AI_RESULT",
                durationMs
        );

        // ========================================
        // 5. AI 판정 결과 DB 저장
        // ========================================
        boolean applied;

        try {
            applied = stateService.apply(
                    messageId,
                    decision,
                    durationMs
            );

        } catch (Exception e) {

            log.error(
                    "[spoiler] 분석 결과 저장 실패 messageId={}",
                    messageId,
                    e
            );

            // DB 저장 실패 단계 기록
            traceService.record(
                    messageId,
                    "DB_FAILED",
                    null
            );

            // PROCESSING으로 남은 작업은 복구 로직에서 재시도
            return;
        }

        // ========================================
        // 6. 기존 journal 로그 유지
        // ========================================
        log.info(
                "[spoiler] messageId={} decision={} applied={} {}ms",
                messageId,
                decision.note(),
                applied,
                durationMs
        );

        // 이미 다른 작업이 처리했거나 상태가 달라졌다면 종료
        if (!applied) {
            return;
        }

        // ========================================
        // 7. DB 저장 성공 단계 기록
        // ========================================
        traceService.record(
                messageId,
                "DB_SAVED",
                null
        );

        // ========================================
        // 8. Redis 갱신 알림
        // ========================================
        try {

            chatRedisPublisher.publish(
                    new ChatRedisEvent(
                            event.roomId(),
                            messageId
                    )
            );

            // Redis 갱신 알림 성공
            traceService.record(
                    messageId,
                    "REDIS_PUBLISHED",
                    null
            );

        } catch (Exception e) {

            log.warn(
                    "[spoiler] 분석 완료 알림 실패 messageId={}",
                    messageId,
                    e
            );

            // Redis 갱신 알림 실패
            traceService.record(
                    messageId,
                    "REDIS_FAILED",
                    null
            );
        }
    }

    /**
     * 작성자 시청 진도를 포함한 AI 스포일러 판정
     *
     * 작성자가 이미 본 회차에 대한 추측인지,
     * 아직 보지 않은 회차에 대한 추측인지 구분한다.
     */
    private AnalysisDecision judge(ChatAnalysisRequested event) {

        // 작성자의 현재 시청 회차 조회
        Integer authorWatched =
                authorProgressService.forChatMessage(
                        event.messageId(),
                        event.dramaId()
                );

        // 시청 진도를 포함하여 AI 판정
        return judgeService.judge(
                event.messageId(),
                event.dramaId(),
                event.content(),
                authorWatched
        );
    }
}

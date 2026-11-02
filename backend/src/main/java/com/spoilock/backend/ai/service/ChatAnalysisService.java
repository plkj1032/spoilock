
package com.spoilock.backend.ai.service;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatAnalysisService {

    private final ChatAnalysisStateService stateService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final SpoilerJudgeService judgeService;

    // 최신 main의 작성자 시청 진도 조회
    private final AuthorProgressService authorProgressService;

    public ChatAnalysisService(
            ChatAnalysisStateService stateService,
            ChatRedisPublisher chatRedisPublisher,
            SpoilerJudgeService judgeService,
            AuthorProgressService authorProgressService
    ) {
        this.stateService = stateService;
        this.chatRedisPublisher = chatRedisPublisher;
        this.judgeService = judgeService;
        this.authorProgressService = authorProgressService;
    }

    public void analyze(ChatAnalysisRequested event) {

        // 1. 분석 작업 선점
        if (!stateService.tryStart(event.messageId())) {
            return;
        }

        // 2. AI 분석 처리시간 측정 시작
        long startedAt = System.nanoTime();

        // 3. AI 판정
        AnalysisDecision decision = judge(event);

        long durationMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );

        // 4. 분석 결과와 모니터링 정보 저장
        boolean applied;

        try {
            applied = stateService.apply(
                    event.messageId(),
                    decision,
                    durationMs
            );
        } catch (Exception e) {
            log.error(
                    "[spoiler] 분석 결과 저장 실패 messageId={}",
                    event.messageId(),
                    e
            );
            return;
        }

        // 5. 로그 출력
        log.info(
                "[spoiler] messageId={} decision={} applied={} {}ms",
                event.messageId(),
                decision.note(),
                applied,
                durationMs
        );

        if (!applied) {
            return;
        }

        // 6. DB 커밋 후 Redis 갱신 알림
        try {
            chatRedisPublisher.publish(
                    new ChatRedisEvent(
                            event.roomId(),
                            event.messageId()
                    )
            );
        } catch (Exception e) {
            log.warn(
                    "[spoiler] 분석 완료 알림 실패 messageId={}",
                    event.messageId(),
                    e
            );
        }
    }

    /**
     * 작성자의 시청 진도를 포함한 AI 판정
     */
    private AnalysisDecision judge(ChatAnalysisRequested event) {

        Integer authorWatched =
                authorProgressService.forChatMessage(
                        event.messageId(),
                        event.dramaId()
                );

        return judgeService.judge(
                event.messageId(),
                event.dramaId(),
                event.content(),
                authorWatched
        );
    }
}

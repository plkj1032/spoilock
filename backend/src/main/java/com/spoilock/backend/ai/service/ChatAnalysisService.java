package com.spoilock.backend.ai.service;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 채팅 1건 스포일러 분석 전체 흐름.
 *
 * 1) PENDING → PROCESSING 선점 (짧은 트랜잭션)
 * 2) Python /judge 호출 (트랜잭션 없음)
 * 3) 결과 → 상태 저장 (짧은 트랜잭션)
 * 4) 같은 messageId 로 Redis 알림 → 클라이언트가 다시 조회해 말풍선 갱신
 *
 * 이 클래스 전체에 @Transactional 을 붙이지 말 것.
 */
@Slf4j
@Service
public class ChatAnalysisService {

    private final ChatAnalysisStateService stateService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final SpoilerJudgeService judgeService;

    public ChatAnalysisService(
            ChatAnalysisStateService stateService,
            ChatRedisPublisher chatRedisPublisher,
            SpoilerJudgeService judgeService
    ) {
        this.stateService = stateService;
        this.chatRedisPublisher = chatRedisPublisher;
        this.judgeService = judgeService;
    }

    public void analyze(ChatAnalysisRequested event) {

        if (!stateService.tryStart(event.messageId())) {
            // 이미 다른 스레드/서버가 처리 중이거나 완료됨
            return;
        }

        long startedAt = System.currentTimeMillis();
        AnalysisDecision decision = judge(event);

        boolean applied;
        try {
            applied = stateService.apply(event.messageId(), decision);
        } catch (Exception e) {
            log.error("[spoiler] 분석 결과 저장 실패 messageId={}", event.messageId(), e);
            return;   // PROCESSING 으로 남음 → 복구 작업이 재시도
        }

        log.info("[spoiler] messageId={} decision={} applied={} {}ms",
                event.messageId(), decision.note(), applied,
                System.currentTimeMillis() - startedAt);

        if (!applied) {
            return;
        }

        // 결과 저장(커밋) 후 알림. 알림 실패는 분석 실패가 아님 (새로고침 시 정상 조회)
        try {
            chatRedisPublisher.publish(
                    new ChatRedisEvent(event.roomId(), event.messageId())
            );
        } catch (Exception e) {
            log.warn("[spoiler] 분석 완료 알림 실패 messageId={}", event.messageId(), e);
        }
    }

    private AnalysisDecision judge(ChatAnalysisRequested event) {
        return judgeService.judge(event.messageId(), event.dramaId(), event.content());
    }
}

package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiClient;
import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.ai.client.SpoilerJudgeResponse;
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

    private final SpoilerApiClient spoilerApiClient;
    private final SpoilerApiProperties properties;
    private final ChatAnalysisStateService stateService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final SpoilerVerdictPolicy policy;
    private final SpoilerKbService kbService;

    public ChatAnalysisService(
            SpoilerApiClient spoilerApiClient,
            SpoilerApiProperties properties,
            ChatAnalysisStateService stateService,
            ChatRedisPublisher chatRedisPublisher,
            SpoilerKbService kbService
    ) {
        this.spoilerApiClient = spoilerApiClient;
        this.properties = properties;
        this.kbService = kbService;
        this.stateService = stateService;
        this.chatRedisPublisher = chatRedisPublisher;
        this.policy = new SpoilerVerdictPolicy(
                properties.allowUnverifiedDemo(),
                properties.publishSkippedGeneral(),
                properties.publishSkippedUnrelated(),
                properties.publishNoMatch(),
                properties.publishOffTopic()
        );
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

        // 작품별 판정 범위: Python 에 사건이 등록된 회차 (관리자 페이지에서 적재·검수)
        SpoilerApiProperties.DramaKb kb = kbService.findDrama(event.dramaId());

        if (kb == null || kb.kbDramaId() == null
                || kb.episodes() == null || kb.episodes().isEmpty()) {
            // 사건 DB 가 없는 작품: 다른 작품 데이터로 판정하지 않고 보류
            return AnalysisDecision.hold("사건 DB 미등록 작품 dramaId=" + event.dramaId());
        }

        try {
            SpoilerJudgeResponse response = spoilerApiClient.judge(
                    event.messageId(),
                    kb.kbDramaId(),
                    event.content(),
                    kb.episodes()
            );
            return policy.decide(response, kb.episodes());

        } catch (Exception e) {
            // 연결 실패·시간 초과·4xx/5xx·잘못된 응답
            log.warn("[spoiler] 분석 서버 호출 실패 messageId={}: {}",
                    event.messageId(), e.toString());
            return AnalysisDecision.failed("호출 실패: " + e.getClass().getSimpleName());
        }
    }
}
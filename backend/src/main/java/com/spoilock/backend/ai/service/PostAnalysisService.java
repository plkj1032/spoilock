package com.spoilock.backend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * 커뮤니티 게시글·댓글 1건 스포일러 분석 (채팅과 같은 판정 범위·공개 정책).
 *
 * 1) PENDING → PROCESSING 선점 (짧은 트랜잭션)
 * 2) Python /judge 호출 (트랜잭션 없음)
 * 3) 결과 → 상태 저장 (짧은 트랜잭션)
 *
 * 피드는 실시간 알림 없이 다음 조회(새로고침) 때 결과가 반영된다.
 * 이 클래스에 @Transactional 을 붙이지 말 것.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostAnalysisService {

    private final PostAnalysisStateService stateService;
    private final SpoilerJudgeService judgeService;

    public void analyze(PostAnalysisRequested event) {

        if (!stateService.tryStart(event.postId())) {
            // 이미 다른 스레드/서버가 처리 중이거나 완료됨
            return;
        }

        long startedAt = System.currentTimeMillis();
        AnalysisDecision decision = judgeService.judge(
                event.postId(), event.dramaId(), event.content()
        );

        boolean applied;
        try {
            applied = stateService.apply(event.postId(), decision);
        } catch (Exception e) {
            log.error("[spoiler] 게시글 분석 결과 저장 실패 postId={}", event.postId(), e);
            return;   // PROCESSING 으로 남음 → 복구 작업이 재시도
        }

        log.info("[spoiler] postId={} decision={} applied={} {}ms",
                event.postId(), decision.note(), applied,
                System.currentTimeMillis() - startedAt);
    }
}

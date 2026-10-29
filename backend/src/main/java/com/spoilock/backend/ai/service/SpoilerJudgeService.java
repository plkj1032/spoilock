package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiClient;
import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.ai.client.SpoilerJudgeResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * 글 1건(채팅·게시글·댓글) 스포일러 판정 → 공개 결정.
 * 채팅과 커뮤니티가 같은 판정 범위·같은 공개 정책을 쓰도록 한 곳에 둔다.
 *
 * Python HTTP 호출을 하므로 DB 트랜잭션 밖에서 호출할 것.
 */
@Slf4j
@Service
public class SpoilerJudgeService {

    private final SpoilerApiClient spoilerApiClient;
    private final SpoilerKbService kbService;
    private final SpoilerVerdictPolicy policy;
    private final boolean publishUnindexedDrama;

    public SpoilerJudgeService(
            SpoilerApiClient spoilerApiClient,
            SpoilerApiProperties properties,
            SpoilerKbService kbService
    ) {
        this.spoilerApiClient = spoilerApiClient;
        this.kbService = kbService;
        this.publishUnindexedDrama = properties.publishUnindexedDrama();
        this.policy = new SpoilerVerdictPolicy(
                properties.allowUnverifiedDemo(),
                properties.publishSkippedGeneral(),
                properties.publishSkippedUnrelated(),
                properties.publishNoMatch(),
                properties.publishOffTopic()
        );
    }

    /**
     * @param logId   Python 로그·응답 확인용 ID (채팅 메시지 ID 또는 게시글 ID)
     * @param dramaId Spring dramas.id
     */
    public AnalysisDecision judge(Long logId, Long dramaId, String content) {

        // 작품별 판정 범위: Python 에 사건이 등록된 회차 (관리자 페이지에서 적재·검수)
        SpoilerKbService.KbLookup lookup = kbService.lookup(dramaId);
        SpoilerApiProperties.DramaKb kb = lookup.kb();

        if (lookup.noEvents()) {
            // 분석 서버가 확인한 '사건이 하나도 없는 작품' → 비교할 스포일러가 없음
            return publishUnindexedDrama
                    ? AnalysisDecision.publishAll("사건 미등록 작품: 판정 대상 없음 dramaId=" + dramaId)
                    : AnalysisDecision.hold("사건 미등록 작품 dramaId=" + dramaId);
        }

        if (kb == null || kb.kbDramaId() == null
                || kb.episodes() == null || kb.episodes().isEmpty()) {
            // 분석 서버에 물어볼 수 없음 → 원문은 가린 채 실패 처리, 복구 작업이 재시도
            return AnalysisDecision.failed("사건 DB 조회 실패 dramaId=" + dramaId);
        }

        try {
            SpoilerJudgeResponse response = spoilerApiClient.judge(
                    logId,
                    kb.kbDramaId(),
                    content,
                    kb.episodes()
            );
            return policy.decide(response, kb.episodes());

        } catch (Exception e) {
            // 연결 실패·시간 초과·4xx/5xx·잘못된 응답
            log.warn("[spoiler] 분석 서버 호출 실패 id={}: {}", logId, e.toString());
            return AnalysisDecision.failed("호출 실패: " + e.getClass().getSimpleName());
        }
    }
}

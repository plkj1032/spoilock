package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiClient;
import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.ai.client.SpoilerJudgeResponse;
import com.spoilock.backend.ai.client.SpoilerJudgeRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
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
    private final SpoilerFinaleService finaleService;
    // 보류 대신 '드라마 마지막 회차부터 공개' (false 면 이전처럼 관리자 검토 대기)
    private final boolean holdAsFinale;
    // 모든 보류를 당분간 전체 공개 (회의 결정 11/3). false 면 holdAsFinale 규칙
    private final boolean publishHeld;

    public SpoilerJudgeService(
            SpoilerApiClient spoilerApiClient,
            SpoilerApiProperties properties,
            SpoilerKbService kbService,
            SpoilerFinaleService finaleService,
            @Value("${app.spoiler.hold-as-finale:true}") boolean holdAsFinale,
            @Value("${app.spoiler.publish-held:true}") boolean publishHeld
    ) {
        this.spoilerApiClient = spoilerApiClient;
        this.kbService = kbService;
        this.finaleService = finaleService;
        this.holdAsFinale = holdAsFinale;
        this.publishHeld = publishHeld;
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
        return judge(logId, dramaId, content, null);
    }

    /**
     * @param authorWatchedEpisode 작성자의 이 작품 시청 회차 (추측성 글 판단용, 모르면 null)
     */
    public AnalysisDecision judge(Long logId, Long dramaId, String content, Integer authorWatchedEpisode) {
        return resolveHold(judgeOnce(logId, dramaId, content, authorWatchedEpisode), dramaId);
    }

    /**
     * 보류 처리 (채팅·게시글 공통)
     *  1. publishHeld=true (기본, 회의 결정 11/3): 모든 보류를 전체 공개
     *  2. publishHeld=false 이고 holdAsFinale=true: 마지막 회차부터 공개
     *  3. 그 외: 보류 유지 (작성자만 보임)
     */
    private AnalysisDecision resolveHold(AnalysisDecision decision, Long dramaId) {
        if (decision == null || decision.failed() || decision.publish()) {
            return decision;
        }

        if (publishHeld) {
            return AnalysisDecision.publishAll(HoldRules.HELD_PUBLISHED_PREFIX + decision.note());
        }

        if (!holdAsFinale) {
            return decision;
        }

        // 보류 → 마지막 회차부터 공개 (끝까지 본 사람만)
        try {
            SpoilerKbService.KbLookup lookup = kbService.lookup(dramaId);
            return SpoilerVerdictPolicy.toFinale(
                    decision,
                    finaleService.finaleEpisode(dramaId),
                    lookup.kb() == null ? null : lookup.kb().episodes()
            );
        } catch (Exception e) {
            log.warn("[spoiler] 마지막 회차 조회 실패 dramaId={}: {}", dramaId, e.toString());
            return decision;   // 보류 유지
        }
    }



    private AnalysisDecision judgeOnce(Long logId, Long dramaId, String content, Integer authorWatchedEpisode) {

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
            return policy.decide(response, kb.episodes(), authorWatchedEpisode);

        } catch (Exception e) {
            // 연결 실패·시간 초과·4xx/5xx·잘못된 응답
            log.warn("[spoiler] 분석 서버 호출 실패 id={}: {}", logId, e.toString());
            return AnalysisDecision.failed("호출 실패: " + e.getClass().getSimpleName());
        }
    }

    // FastAPI 원본 판정 + Spring 최종 공개 결정
    public record ChatJudgeResult(
            AnalysisDecision decision,
            String rawVerdict
    ){}

    // 기준 호출과의 호환성 유지
    public AnalysisDecision judgeChat(
            Long messageId,
            Long dramaId,
            String content,
            Integer authorWatchedEpisode
    )
    {
        return judgeChatWithVerdict(
                messageId,
                dramaId,
                content,
                authorWatchedEpisode
        ).decision();
    }

    // 관리자 모니터링을 위한 원본 판정 수집 (직전 대화 없이)
    public ChatJudgeResult judgeChatWithVerdict(
            Long messageId,
            Long dramaId,
            String content,
            Integer authorWatchedEpisode
    ) {
        return judgeChatWithVerdict(messageId, dramaId, content, authorWatchedEpisode, List.of());
    }

    /**
     * @param context 같은 채팅방 직전 대화 (질문→답, 나눠 보낸 문장 판정용). 비어 있으면 메시지만 판정
     */
    public ChatJudgeResult judgeChatWithVerdict(
            Long messageId,
            Long dramaId,
            String content,
            Integer authorWatchedEpisode,
            List<SpoilerJudgeRequest.ContextLine> context
    ) {
        SpoilerKbService.KbLookup lookup =
                kbService.lookup(dramaId);

        SpoilerApiProperties.DramaKb kb = lookup.kb();

        if (lookup.noEvents()) {
            AnalysisDecision decision = publishUnindexedDrama
                    ? AnalysisDecision.publishAll(
                    "사건 미등록 작품: 판정 대상 없음 dramaId="
                    + dramaId
            )
                    : AnalysisDecision.hold(
                    "사건 미등록 작품 dramaId=" + dramaId
            );

            // FastAPI /judge 자체를 호출하지 않았음
            return new ChatJudgeResult(resolveHold(decision, dramaId), null);
        }

        if (kb == null
                || kb.kbDramaId() == null
                || kb.episodes() == null
                || kb.episodes().isEmpty()) {

            return new ChatJudgeResult(
                    AnalysisDecision.failed(
                            "사건 DB 조회 실패 dramaId=" + dramaId
                    ),
                    null
            );
        }

        try {
            SpoilerJudgeResponse response =
                    spoilerApiClient.judge(
                            messageId,
                            kb.kbDramaId(),
                            content,
                            kb.episodes(),
                            context
                    );

            AnalysisDecision decision =
                    policy.decideChat(
                            response,
                            kb.episodes(),
                            authorWatchedEpisode
                    ).withAiResponse(response);

            String rawVerdict = response == null
                    ? null
                    : normalizeVerdict(response.verdict());

            return new ChatJudgeResult(
                    resolveHold(decision, dramaId),
                    rawVerdict
            );

        } catch (Exception e) {
            log.warn(
                    "[spoiler] 채팅 분석 실패 messageId={}: {}",
                    messageId,
                    e.toString()
            );

            return new ChatJudgeResult(
                    AnalysisDecision.failed(
                            "호출 실패: "
                                    + e.getClass().getSimpleName()
                    ),
                    null
            );
        }
    }

    // 관리자 DB에는 허용된 판정 코드만 저장
    private String normalizeVerdict(String verdict) {
        if (verdict == null) {
            return null;
        }

        return switch (verdict) {
            case "MATCH",
                 "NO_MATCH",
                 "UNCERTAIN",
                 "ERROR",
                 "SKIPPED_GENERAL",
                 "SKIPPED_UNRELATED" -> verdict;

            default -> "UNKNOWN_VERDICT";
        };
    }
}
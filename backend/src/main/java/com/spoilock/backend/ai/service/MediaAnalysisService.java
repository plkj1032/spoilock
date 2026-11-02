package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.ImageJudgeResponse;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.post.service.PostMediaStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * 게시글 이미지 1장 스포일러 판정.
 *
 * 1) PENDING/FAILED → PROCESSING 선점 (짧은 트랜잭션)
 * 2) 작품의 판정 범위 조회 → 사건이 없는 작품이면 바로 공개
 * 3) 이미지를 Python /admin/judge-image 로 전송 (트랜잭션 없음)
 *    - 영상 + 자막 분석 사건이 없는 작품은 Python 이 SKIPPED 로 답함 → 공개
 * 4) 결과 저장: 블러 기준 회차 / 실패(블러 유지, 복구 작업이 재시도)
 *
 * 이 클래스에 @Transactional 을 붙이지 말 것.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAnalysisService {

    private final MediaAnalysisStateService stateService;
    private final SpoilerKbService kbService;
    private final SpoilerAdminClient adminClient;
    private final PostMediaStorage storage;

    public void analyze(Long mediaId) {

        if (!stateService.tryStart(mediaId)) {
            return;   // 이미 다른 스레드/서버가 처리 중이거나 완료
        }

        long startedAt = System.currentTimeMillis();
        ImageVerdictPolicy.ImageDecision decision;

        try {
            decision = judge(mediaId);
        } catch (Exception e) {
            log.warn("[spoiler] 이미지 판정 실패 mediaId={}: {}", mediaId, e.toString());
            decision = ImageVerdictPolicy.ImageDecision.failed("호출 실패: " + e.getClass().getSimpleName());
        }

        boolean applied;
        try {
            applied = stateService.apply(mediaId, decision);
        } catch (Exception e) {
            log.error("[spoiler] 이미지 판정 결과 저장 실패 mediaId={}", mediaId, e);
            return;   // PROCESSING 으로 남음 → 복구 작업이 재시도
        }

        log.info("[spoiler] mediaId={} decision={} blurUntil={} applied={} {}ms",
                mediaId, decision.note(), decision.requiredEpisode(), applied,
                System.currentTimeMillis() - startedAt);
    }

    private ImageVerdictPolicy.ImageDecision judge(Long mediaId) {

        MediaAnalysisStateService.Target target = stateService.load(mediaId);
        if (target == null) {
            return ImageVerdictPolicy.ImageDecision.failed("이미지 없음");
        }

        SpoilerKbService.KbLookup lookup = kbService.lookup(target.dramaId());

        if (lookup.noEvents()) {
            return ImageVerdictPolicy.ImageDecision.showAll("사건 미등록 작품");
        }

        SpoilerApiProperties.DramaKb kb = lookup.kb();
        if (kb == null || kb.kbDramaId() == null || kb.episodes() == null || kb.episodes().isEmpty()) {
            return ImageVerdictPolicy.ImageDecision.failed("사건 DB 조회 실패");
        }

        byte[] image = storage.read(target.storageKey());

        ImageJudgeResponse response = adminClient.judgeImage(
                image,
                target.contentType(),
                kb.kbDramaId(),
                kb.episodes(),
                mediaId
        );

        return ImageVerdictPolicy.decide(response, kb.episodes());
    }
}

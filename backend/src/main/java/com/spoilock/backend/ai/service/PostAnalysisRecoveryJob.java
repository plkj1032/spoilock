package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글·댓글 분석이 유실된 경우 복구 (ChatAnalysisRecoveryJob 과 같은 설정 사용).
 * - 서버 재시작·대기열 초과로 처리되지 못한 PENDING (이 기능 이전에 작성된 글 포함)
 * - 분석 도중 서버가 죽어 남은 PROCESSING
 * - Python 서버 중단·Gemini 일시 오류로 FAILED 된 최근 글 (재시도)
 *
 * 여러 Spring 서버가 동시에 실행해도 tryStart() 의 조건부 UPDATE 로 한 곳만 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostAnalysisRecoveryJob {

    // 관리자가 숨김/삭제한 글은 다시 분석하지 않음
    private static final List<ModerationStatus> TARGET_MODERATION =
            List.of(ModerationStatus.PENDING, ModerationStatus.VISIBLE);

    private final PostRepository postRepository;
    private final PostAnalysisStateService stateService;
    private final PostAnalysisService postAnalysisService;
    private final SpoilerApiProperties properties;

    @Scheduled(
            initialDelayString = "${app.spoiler.recovery.interval-ms:30000}",
            fixedDelayString = "${app.spoiler.recovery.interval-ms:30000}"
    )
    public void recover() {
        SpoilerApiProperties.Recovery recovery = properties.recovery();

        if (recovery == null || !recovery.enabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<PostAnalysisTarget> targets = postRepository.findAnalysisTargets(
                TARGET_MODERATION,
                AnalysisStatus.PENDING,
                now.minusSeconds(recovery.pendingAfterSeconds()),
                AnalysisStatus.PROCESSING,
                now.minusSeconds(recovery.processingAfterSeconds()),
                AnalysisStatus.FAILED,
                now.minusMinutes(recovery.retryFailedWithinMinutes()),
                PageRequest.of(0, recovery.batchSize())
        );

        if (targets.isEmpty()) {
            return;
        }

        log.info("[spoiler] 게시글 복구 대상 {}건", targets.size());

        for (PostAnalysisTarget target : targets) {
            try {
                if (target.analysisStatus() == AnalysisStatus.PROCESSING) {
                    stateService.releaseStuck(target.postId());
                }
                postAnalysisService.analyze(target.toEvent());
            } catch (Exception e) {
                log.warn("[spoiler] 게시글 복구 분석 실패 postId={}", target.postId(), e);
            }
        }
    }
}

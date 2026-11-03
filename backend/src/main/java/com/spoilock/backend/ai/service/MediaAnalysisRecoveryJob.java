package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.ai.client.SpoilerHealthProbe;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.repository.PostMediaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이미지 판정 복구·재시도 (게시글 복구 작업과 같은 주기).
 * - 처리되지 못한 PENDING (이 기능 이전에 올린 이미지 포함)
 * - 분석 도중 서버가 죽어 남은 PROCESSING
 * - 실패(FAILED)한 이미지: 1분 간격으로 최대 10번까지 재시도 (그동안 블러 유지)
 *
 * 여러 Spring 서버가 동시에 실행해도 tryStart() 의 조건부 UPDATE 로 한 곳만 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaAnalysisRecoveryJob {

    static final int MAX_ATTEMPTS = 10;
    static final long RETRY_DELAY_SECONDS = 60;

    private final PostMediaRepository mediaRepository;
    private final MediaAnalysisStateService stateService;
    private final MediaAnalysisService mediaAnalysisService;
    private final SpoilerApiProperties properties;
    private final SpoilerHealthProbe healthProbe;

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

        List<Long> targets = mediaRepository.findAnalysisTargets(
                ModerationStatus.VISIBLE,
                AnalysisStatus.PENDING,
                now.minusSeconds(recovery.pendingAfterSeconds()),
                AnalysisStatus.PROCESSING,
                now.minusSeconds(recovery.processingAfterSeconds()),
                AnalysisStatus.FAILED,
                now.minusSeconds(RETRY_DELAY_SECONDS),
                MAX_ATTEMPTS,
                PageRequest.of(0, recovery.batchSize())
        );

        if (targets.isEmpty()) {
            return;
        }

        if (!healthProbe.isUp()) {
            // 분석 서버가 꺼져 있으면 이번 주기는 건너뜀 (재시도 횟수도 쓰지 않음)
            return;
        }

        log.info("[spoiler] 이미지 판정 복구 대상 {}건", targets.size());

        for (Long mediaId : targets) {
            try {
                // 멈춘 PROCESSING 은 먼저 PENDING 으로 되돌림 (해당 없으면 아무 일 없음)
                stateService.releaseStuck(mediaId);
                mediaAnalysisService.analyze(mediaId);
            } catch (Exception e) {
                log.warn("[spoiler] 이미지 판정 복구 실패 mediaId={}", mediaId, e);
            }
        }
    }
}

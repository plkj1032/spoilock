package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbIngestJob;
import com.spoilock.backend.ai.client.KbVideoJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.ai.client.SpoilerHealthProbe;
import com.spoilock.backend.ai.service.SpoilerFinaleService;
import com.spoilock.backend.ai.service.SpoilerKbService;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.spoiler.entity.EpisodeAnalysis;
import com.spoilock.backend.spoiler.enums.AnalysisSource;
import com.spoilock.backend.spoiler.repository.ActiveAnalysisJob;
import com.spoilock.backend.spoiler.repository.EpisodeAnalysisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 대기·진행 중인 Python 분석 작업(자막 / 영상)의 상태를 주기적으로 받아 episode_analysis 에 반영.
 * 여러 Spring 서버가 동시에 실행해도 같은 값을 덮어쓸 뿐이라 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubtitleAnalysisPoller {

    // 업로드 제한 시간(10분)보다 넉넉하게
    private static final long UPLOAD_STALE_MINUTES = 20;

    private static final List<AnalysisStatus> ACTIVE =
            List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING);

    private final EpisodeAnalysisRepository analysisRepository;
    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;
    private final SpoilerKbService kbService;
    private final SpoilerFinaleService finaleService;
    private final SpoilerHealthProbe healthProbe;

    @Scheduled(
            initialDelayString = "${app.spoiler.admin.poll-interval-ms:5000}",
            fixedDelayString = "${app.spoiler.admin.poll-interval-ms:5000}"
    )
    public void poll() {

        List<ActiveAnalysisJob> jobs = analysisRepository.findActiveJobs(ACTIVE);

        // 분석 서버 상태는 조회할 작업이 있을 때만, 주기당 최대 1번 확인
        Boolean serverUp = null;

        for (ActiveAnalysisJob active : jobs) {

            // 영상을 분석 서버로 보내는 중 (아직 작업 ID 없음) → 조회하지 않고, 너무 오래되면 실패 처리
            if (active.jobId().startsWith(EpisodeAnalysis.UPLOADING_PREFIX)) {
                try {
                    stateService.expireUploadIfStale(active.episodeId(), active.jobId(), UPLOAD_STALE_MINUTES);
                } catch (Exception ex) {
                    log.warn("영상 전달 상태 정리 실패: episodeId={}", active.episodeId(), ex);
                }
                continue;
            }

            if (serverUp == null) {
                serverUp = healthProbe.isUp();
            }
            if (!serverUp) {
                // 분석 서버가 꺼져 있으면 조회하지 않음 (다시 켜지면 작업 없음 응답으로 정리됨)
                continue;
            }

            try {
                // 영상 작업은 음성 인식 + 사건 추출을 합친 상태를 자막 작업 형태로 변환
                KbIngestJob job = active.source() == AnalysisSource.VIDEO
                        ? toView(adminClient.getVideoJob(active.jobId()))
                        : adminClient.getIngestJob(active.jobId());

                if (job == null) {
                    continue;
                }

                boolean completed = stateService.applyJob(
                        active.episodeId(), active.jobId(), job
                );

                if (completed) {
                    // 새 회차가 판정 범위에 들어올 수 있으므로 캐시 갱신
                    kbService.evictAll();
                    // 새 사건이 생겼으므로 '마지막 회차 기준'으로 공개한 최근 글 다시 판정
                    analysisRepository.findKbRef(active.episodeId())
                            .ifPresent(ref -> finaleService.requeueRecent(ref.dramaId()));
                }

            } catch (SpoilerAdminException ex) {
                if (ex.isNotFound()) {
                    stateService.markJobLost(active.episodeId(), active.jobId());
                } else {
                    // 분석 서버 일시 중단 등 → 다음 주기에 다시 조회
                    log.debug("자막 분석 상태 조회 실패: jobId={}, {}",
                            active.jobId(), ex.getReason());
                }
            } catch (Exception ex) {
                log.warn("자막 분석 상태 반영 실패: episodeId={}, jobId={}",
                        active.episodeId(), active.jobId(), ex);
            }
        }
    }

    private static KbIngestJob toView(KbVideoJob job) {
        return job == null ? null : job.toIngestView();
    }
}

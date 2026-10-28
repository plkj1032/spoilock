package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbIngestJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.ai.service.SpoilerKbService;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.spoiler.repository.ActiveAnalysisJob;
import com.spoilock.backend.spoiler.repository.EpisodeAnalysisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 대기·진행 중인 Python 자막 분석 작업의 상태를 주기적으로 받아 episode_analysis 에 반영.
 * 여러 Spring 서버가 동시에 실행해도 같은 값을 덮어쓸 뿐이라 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubtitleAnalysisPoller {

    private static final List<AnalysisStatus> ACTIVE =
            List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING);

    private final EpisodeAnalysisRepository analysisRepository;
    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;
    private final SpoilerKbService kbService;

    @Scheduled(
            initialDelayString = "${app.spoiler.admin.poll-interval-ms:5000}",
            fixedDelayString = "${app.spoiler.admin.poll-interval-ms:5000}"
    )
    public void poll() {

        List<ActiveAnalysisJob> jobs = analysisRepository.findActiveJobs(ACTIVE);

        for (ActiveAnalysisJob active : jobs) {
            try {
                KbIngestJob job = adminClient.getIngestJob(active.jobId());

                if (job == null) {
                    continue;
                }

                boolean completed = stateService.applyJob(
                        active.episodeId(), active.jobId(), job
                );

                if (completed) {
                    // 새 회차가 판정 범위에 들어올 수 있으므로 캐시 갱신
                    kbService.evictAll();
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
}


package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbVideoJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.spoiler.dto.AdminEpisodeAiResponse;
import com.spoilock.backend.spoiler.enums.AnalysisSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


/**
 * MP4 영상 기반 AI 분석 서비스.
 *
 * 1. 분석 가능 여부 확인
 * 2. 요청 접수 후 바로 응답
 * 3. S3 다운로드 → FastAPI 영상 분석 작업 요청 → 작업 ID 저장은 AnalysisUploadWorker(별도 스레드)
 *
 * 영상 전송 중 DB 트랜잭션을 유지하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAnalysisService {

    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;
    private final AnalysisUploadWorker uploadWorker;

    // ==========================================
    // 1. 영상 AI 분석 요청
    // ==========================================
    public AdminEpisodeAiResponse requestAnalysis(
            Long episodeId,
            boolean replace
    ) {

        SubtitleAnalysisStateService.Target target =
                stateService.prepareVideo(episodeId);

        // 이미 분석 완료된 영상은 재분석 요청 필요
        if (target.requestedBefore()
                && target.status() == AnalysisStatus.COMPLETED
                && !replace) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 분석이 완료된 회차입니다. "
                            + "재분석으로 요청해 주세요."
            );
        }

        // ==========================================
        // 이전 실패 작업 재시도
        // ==========================================
        if (!replace
                && target.status() == AnalysisStatus.FAILED
                && target.previousSource() == AnalysisSource.VIDEO
                && target.previousJobId() != null) {

            KbVideoJob resumed =
                    tryResume(target.previousJobId());

            if (resumed != null) {

                return stateService.markStarted(
                        episodeId,
                        resumed.jobId(),
                        AnalysisSource.VIDEO
                );
            }
        }

        // ==========================================
        // 요청은 바로 접수하고, S3 다운로드·분석 서버 전달은 별도 스레드에서
        // (큰 MP4 를 올리는 동안 화면 요청이 시간 제한에 걸리지 않게)
        // ==========================================
        SubtitleAnalysisStateService.UploadTicket ticket =
                stateService.markUploading(episodeId, AnalysisSource.VIDEO);

        try {
            uploadWorker.sendVideo(
                    episodeId,
                    ticket.token(),
                    target.dramaId(),
                    target.episodeNumber(),
                    target.videoStorageKey(),
                    replace
            );
        } catch (TaskRejectedException ex) {
            stateService.markUploadFailed(episodeId, ticket.token(),
                    "영상 전달 대기열이 가득 찼습니다. 잠시 후 다시 요청해 주세요.");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "영상 전달 대기열이 가득 찼습니다. 잠시 후 다시 요청해 주세요."
            );
        }

        return ticket.response();
    }

    // ==========================================
    // 2. 실패한 영상 분석 작업 재시도
    // ==========================================
    private KbVideoJob tryResume(String jobId) {

        try {

            KbVideoJob job =
                    adminClient.retryVideoJob(jobId);

            if (job != null
                    && job.jobId() != null
                    && !"DONE".equals(job.status())
                    && !"FAILED".equals(job.status())) {

                log.info(
                        "영상 AI 분석 작업 재시도: jobId={}",
                        jobId
                );

                return job;
            }

            return null;

        } catch (SpoilerAdminException ex) {

            if (ex.isNotFound() || ex.isConflict()) {
                return null;
            }

            throw ex;
        }
    }
}

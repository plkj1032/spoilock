
package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbVideoJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.storage.S3StorageService;
import com.spoilock.backend.spoiler.dto.AdminEpisodeAiResponse;
import com.spoilock.backend.spoiler.enums.AnalysisSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MP4 영상 기반 AI 분석 서비스.
 *
 * 1. 분석 가능 여부 확인
 * 2. S3에서 MP4 다운로드
 * 3. FastAPI 영상 분석 작업 요청
 * 4. 작업 ID 저장
 * 5. 임시 MP4 파일 삭제
 *
 * 영상 전송 중 DB 트랜잭션을 유지하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAnalysisService {

    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;
    private final S3StorageService s3StorageService;

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
        // S3에서 MP4 다운로드
        // ==========================================
        String s3Key = "videos/" + target.videoStorageKey();

        Path tempVideo = s3StorageService
                .downloadToTemporaryFile(
                        s3Key,
                        ".mp4"
                );

        try {

            // ======================================
            // FastAPI 영상 분석 요청
            // ======================================
            KbVideoJob job;

            try {

                job = adminClient.createVideoJob(
                        tempVideo,
                        target.dramaId(),
                        episodeId,
                        target.episodeNumber(),
                        replace
                );

            } catch (SpoilerAdminException ex) {

                if (ex.upstreamStatus() == 400
                        || ex.upstreamStatus() == 413) {

                    stateService.markRequestFailed(
                            episodeId,
                            ex.getReason()
                    );
                }

                throw ex;
            }

            // ======================================
            // FastAPI 작업 생성 결과 검증
            // ======================================
            if (job == null || job.jobId() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "분석 서버 응답에 작업 ID가 없습니다."
                );
            }

            log.info(
                    "영상 AI 분석 요청 성공: episodeId={}, jobId={}",
                    episodeId,
                    job.jobId()
            );

            return stateService.markStarted(
                    episodeId,
                    job.jobId(),
                    AnalysisSource.VIDEO
            );

        } finally {

            // ======================================
            // 임시 MP4 파일 정리
            // ======================================
            try {

                Files.deleteIfExists(tempVideo);

            } catch (IOException ex) {

                log.warn(
                        "임시 MP4 파일 삭제 실패: {}",
                        tempVideo,
                        ex
                );
            }
        }
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

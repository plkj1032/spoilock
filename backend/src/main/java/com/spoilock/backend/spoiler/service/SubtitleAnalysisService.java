
package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbIngestJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.storage.S3StorageService;
import com.spoilock.backend.drama.repository.EpisodeVideoRepository;
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
 * 자막 또는 자막 + 영상을 FastAPI에 전달해 AI 분석 작업 생성.
 *
 * 1. 분석 가능 여부 확인
 * 2. S3에서 자막 및 선택적 영상 다운로드
 * 3. FastAPI 분석 작업 요청
 * 4. 작업 ID 저장
 * 5. 임시 영상 파일 정리
 *
 * 외부 HTTP 호출 중 DB 연결을 유지하지 않도록
 * 클래스에 @Transactional을 붙이지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleAnalysisService {

    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;
    private final EpisodeVideoRepository videoRepository;
    private final S3StorageService s3StorageService;

    // ==========================================
    // 1. 자막만 분석
    // ==========================================
    public AdminEpisodeAiResponse requestAnalysis(
            Long episodeId,
            boolean replace
    ) {
        return requestAnalysis(episodeId, replace, false);
    }

    // ==========================================
    // 2. 자막 / 자막 + 영상 분석
    // ==========================================
    public AdminEpisodeAiResponse requestAnalysis(
            Long episodeId,
            boolean replace,
            boolean withVideo
    ) {

        SubtitleAnalysisStateService.Target target =
                stateService.prepare(episodeId);

        AnalysisSource source = withVideo
                ? AnalysisSource.SUBTITLE_VIDEO
                : AnalysisSource.SUBTITLE;

        // 이미 완료한 분석이면 재분석 요청 필요
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
        AnalysisSource previous =
                target.previousSource() == null
                        ? AnalysisSource.SUBTITLE
                        : target.previousSource();

        if (!replace
                && target.status() == AnalysisStatus.FAILED
                && previous == source
                && target.previousJobId() != null) {

            KbIngestJob resumed =
                    tryResume(target.previousJobId());

            if (resumed != null) {
                return stateService.markStarted(
                        episodeId,
                        resumed.jobId(),
                        source
                );
            }
        }

        // ==========================================
        // S3에서 SRT 파일 읽기
        // ==========================================
        byte[] srt = readSubtitle(episodeId);

        Path tempVideo = null;

        try {

            // ======================================
            // 영상 함께 분석하는 경우
            // ======================================
            if (withVideo) {

                String storageKey = videoRepository
                        .findByEpisode_Id(episodeId)
                        .map(v -> v.getStorageKey())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "등록된 MP4 영상이 없습니다."
                                )
                        );

                // S3 → 임시 파일 다운로드
                tempVideo = s3StorageService
                        .downloadToTemporaryFile(
                                "videos/" + storageKey,
                                ".mp4"
                        );
            }

            // ======================================
            // FastAPI 분석 요청
            // ======================================
            KbIngestJob job;

            try {

                job = adminClient.createIngestJob(
                        srt,
                        target.dramaId(),
                        episodeId,
                        target.episodeNumber(),
                        replace,
                        tempVideo
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

            // FastAPI 작업 생성 결과 검증
            if (job == null || job.jobId() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "분석 서버 응답에 작업 ID가 없습니다."
                );
            }

            log.info(
                    "AI 분석 요청 성공: episodeId={}, source={}, jobId={}",
                    episodeId,
                    source,
                    job.jobId()
            );

            return stateService.markStarted(
                    episodeId,
                    job.jobId(),
                    source
            );

        } finally {

            // ======================================
            // 분석 요청 종료 후 임시 MP4 삭제
            // ======================================
            if (tempVideo != null) {

                try {
                    Files.deleteIfExists(tempVideo);

                } catch (IOException ex) {

                    log.warn(
                            "임시 MP4 파일 정리 실패: {}",
                            tempVideo,
                            ex
                    );
                }
            }
        }
    }

    // ==========================================
    // 3. S3에서 SRT 자막 조회
    // ==========================================
    private byte[] readSubtitle(Long episodeId) {

        String key = "subtitles/" + episodeId + ".srt";

        return s3StorageService.readBytes(key);
    }

    // ==========================================
    // 4. 실패한 AI 분석 작업 재시도
    // ==========================================
    private KbIngestJob tryResume(String jobId) {

        try {

            KbIngestJob job =
                    adminClient.retryIngestJob(jobId);

            if (job != null
                    && job.jobId() != null
                    && !job.isDone()
                    && !job.isFailed()) {

                log.info(
                        "자막 분석 작업 재시도: jobId={}",
                        jobId
                );

                return job;
            }

            return null;

        } catch (SpoilerAdminException ex) {

            // 기존 작업이 없거나 재시도 불가능하면
            // 신규 작업 생성으로 진행
            if (ex.isNotFound() || ex.isConflict()) {
                return null;
            }

            throw ex;
        }
    }
}

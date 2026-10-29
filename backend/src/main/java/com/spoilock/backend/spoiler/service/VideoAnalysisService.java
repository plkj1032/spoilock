package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbVideoJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.spoiler.dto.AdminEpisodeAiResponse;
import com.spoilock.backend.spoiler.enums.AnalysisSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * "영상으로 분석" 버튼 → Python 영상 분석 작업 생성.
 * Python 이 음성 인식(Whisper)으로 자막을 만든 뒤 자막 분석과 같은 흐름으로 사건을 추출한다.
 *
 * 1) 요청 가능 여부 확인 (짧은 트랜잭션)
 * 2) Python 으로 MP4 전송 (트랜잭션 없음)
 * 3) 작업 ID 저장 (짧은 트랜잭션)
 * 진행 상태는 SubtitleAnalysisPoller 가 주기적으로 받아온다.
 *
 * 이 클래스에 @Transactional 을 붙이지 말 것 (영상 전송 동안 DB 연결을 잡지 않도록).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAnalysisService {

    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;

    @Value("${app.video-storage-dir:./private/videos}")
    private String videoStorageDir;

    /**
     * @param replace 이미 사건이 등록된 회차를 다시 분석 (미검수 사건 교체, 검수·수동 사건 보존)
     */
    public AdminEpisodeAiResponse requestAnalysis(Long episodeId, boolean replace) {

        SubtitleAnalysisStateService.Target target = stateService.prepareVideo(episodeId);

        if (target.requestedBefore()
                && target.status() == AnalysisStatus.COMPLETED
                && !replace) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 분석이 완료된 회차입니다. 다시 분석하려면 재분석으로 요청해 주세요."
            );
        }

        // 실패한 영상 작업은 이어서 진행 (사건 추출만 실패했으면 음성 인식을 다시 하지 않음)
        if (!replace
                && target.status() == AnalysisStatus.FAILED
                && target.previousSource() == AnalysisSource.VIDEO
                && target.previousJobId() != null) {

            KbVideoJob resumed = tryResume(target.previousJobId());

            if (resumed != null) {
                return stateService.markStarted(episodeId, resumed.jobId(), AnalysisSource.VIDEO);
            }
        }

        Path video = videoPath(target.videoStorageKey());

        KbVideoJob job;
        try {
            job = adminClient.createVideoJob(
                    video,
                    target.dramaId(),
                    episodeId,
                    target.episodeNumber(),
                    replace
            );
        } catch (SpoilerAdminException ex) {
            if (ex.upstreamStatus() == 400 || ex.upstreamStatus() == 413) {
                stateService.markRequestFailed(episodeId, ex.getReason());
            }
            throw ex;
        }

        if (job == null || job.jobId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "분석 서버 응답에 작업 ID가 없습니다."
            );
        }

        return stateService.markStarted(episodeId, job.jobId(), AnalysisSource.VIDEO);
    }

    /** 이전 실패 작업 재시도. 재시도할 수 없으면 null (새 작업 생성) */
    private KbVideoJob tryResume(String jobId) {
        try {
            KbVideoJob job = adminClient.retryVideoJob(jobId);

            if (job != null && job.jobId() != null
                    && !"DONE".equals(job.status()) && !"FAILED".equals(job.status())) {
                log.info("영상 분석 작업 재시도: jobId={}", jobId);
                return job;
            }
            return null;

        } catch (SpoilerAdminException ex) {
            // 404: Python 에 작업 기록 없음 / 409: 재시도할 수 없는 상태 → 새 작업으로 진행
            if (ex.isNotFound() || ex.isConflict()) {
                return null;
            }
            throw ex;
        }
    }

    private Path videoPath(String storageKey) {

        Path directory = Path.of(videoStorageDir).toAbsolutePath().normalize();
        Path target = directory.resolve(storageKey).normalize();

        if (!target.startsWith(directory) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "저장된 영상 파일을 찾을 수 없습니다."
            );
        }
        return target;
    }
}

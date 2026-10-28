package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbIngestJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.spoiler.dto.AdminEpisodeAiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * "분석 요청" 버튼 → Python 자막 분석 작업 생성.
 *
 * 1) 요청 가능 여부 확인 (짧은 트랜잭션)
 * 2) Python 호출 (트랜잭션 없음)
 * 3) 작업 ID 저장 (짧은 트랜잭션)
 * 진행 상태는 SubtitleAnalysisPoller 가 주기적으로 받아온다.
 *
 * 이 클래스에 @Transactional 을 붙이지 말 것 (HTTP 호출 동안 DB 연결을 잡지 않도록).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleAnalysisService {

    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;

    @Value("${app.subtitle-storage-dir:./private/subtitles}")
    private String subtitleStorageDir;

    /**
     * @param replace 이미 사건이 등록된 회차를 다시 분석 (미검수 사건 교체, 검수·수동 사건 보존)
     */
    public AdminEpisodeAiResponse requestAnalysis(Long episodeId, boolean replace) {

        SubtitleAnalysisStateService.Target target = stateService.prepare(episodeId);

        if (target.requestedBefore()
                && target.status() == AnalysisStatus.COMPLETED
                && !replace) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 분석이 완료된 회차입니다. 다시 분석하려면 재분석으로 요청해 주세요."
            );
        }

        // 실패한 작업은 같은 자막이면 Python 체크포인트에서 이어서 진행 (끝난 Gemini 배치 재호출 없음)
        if (!replace
                && target.status() == AnalysisStatus.FAILED
                && target.previousJobId() != null) {

            KbIngestJob resumed = tryResume(target.previousJobId());

            if (resumed != null) {
                return stateService.markStarted(episodeId, resumed);
            }
        }

        byte[] srt = readSubtitle(episodeId);

        KbIngestJob job;
        try {
            job = adminClient.createIngestJob(
                    srt,
                    target.dramaId(),
                    episodeId,
                    target.episodeNumber(),
                    replace
            );
        } catch (SpoilerAdminException ex) {
            if (ex.upstreamStatus() == 400) {
                // 자막 형식 문제는 화면에 남겨 관리자가 원인을 볼 수 있게
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

        return stateService.markStarted(episodeId, job);
    }

    /** 이전 실패 작업 재시도. 재시도할 수 없으면 null (새 작업 생성) */
    private KbIngestJob tryResume(String jobId) {
        try {
            KbIngestJob job = adminClient.retryIngestJob(jobId);

            if (job != null && job.jobId() != null && !job.isDone() && !job.isFailed()) {
                log.info("자막 분석 작업 재시도: jobId={}", jobId);
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

    private byte[] readSubtitle(Long episodeId) {

        Path target = Path.of(subtitleStorageDir)
                .toAbsolutePath()
                .normalize()
                .resolve(episodeId + ".srt");

        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "저장된 자막 파일을 찾을 수 없습니다."
            );
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "자막 파일을 읽을 수 없습니다.",
                    ex
            );
        }
    }
}

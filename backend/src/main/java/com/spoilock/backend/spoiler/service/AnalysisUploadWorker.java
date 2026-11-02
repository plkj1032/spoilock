package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbIngestJob;
import com.spoilock.backend.ai.client.KbVideoJob;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerAdminException;
import com.spoilock.backend.common.storage.S3StorageService;
import com.spoilock.backend.spoiler.enums.AnalysisSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 영상이 포함된 분석 요청의 '전달' 단계 (별도 스레드).
 *
 * S3 에서 MP4 를 내려받아 분석 서버로 올리는 데 수십 초 ~ 몇 분이 걸려,
 * 관리자 요청 안에서 기다리면 프록시(nginx·로드밸런서) 시간 제한에 걸려 화면에는 실패로 보이는데
 * 실제로는 분석이 시작되는 문제가 있었다.
 * 그래서 요청은 바로 접수(202)하고, 전달은 여기서 끝낸 뒤 결과를 DB 에 기록한다.
 * 진행 상황은 기존처럼 SubtitleAnalysisPoller 가 이어서 반영한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisUploadWorker {

    public static final String EXECUTOR = "analysisUploadExecutor";

    private final SubtitleAnalysisStateService stateService;
    private final SpoilerAdminClient adminClient;
    private final S3StorageService s3StorageService;

    /** 자막 + 영상 */
    @Async(EXECUTOR)
    public void sendSubtitleWithVideo(
            Long episodeId,
            String token,
            byte[] srt,
            Long dramaId,
            int episodeNumber,
            String videoStorageKey,
            boolean replace
    ) {
        run(episodeId, token, AnalysisSource.SUBTITLE_VIDEO, videoStorageKey, video -> {
            KbIngestJob job = adminClient.createIngestJob(
                    srt, dramaId, episodeId, episodeNumber, replace, video);
            return job == null ? null : job.jobId();
        });
    }

    /** 영상만 (음성 인식) */
    @Async(EXECUTOR)
    public void sendVideo(
            Long episodeId,
            String token,
            Long dramaId,
            int episodeNumber,
            String videoStorageKey,
            boolean replace
    ) {
        run(episodeId, token, AnalysisSource.VIDEO, videoStorageKey, video -> {
            KbVideoJob job = adminClient.createVideoJob(
                    video, dramaId, episodeId, episodeNumber, replace);
            return job == null ? null : job.jobId();
        });
    }

    private interface Sender {
        String send(Path video);
    }

    private void run(Long episodeId, String token, AnalysisSource source, String videoStorageKey, Sender sender) {

        long startedAt = System.currentTimeMillis();
        Path video = null;

        try {
            video = s3StorageService.downloadToTemporaryFile("videos/" + videoStorageKey, ".mp4");

            String jobId = sender.send(video);

            if (jobId == null) {
                stateService.markUploadFailed(episodeId, token, "분석 서버 응답에 작업 ID가 없습니다.");
                return;
            }

            stateService.markUploaded(episodeId, token, jobId, source);
            log.info("영상 전달 완료: episodeId={}, source={}, {}ms",
                    episodeId, source, System.currentTimeMillis() - startedAt);

        } catch (SpoilerAdminException ex) {
            log.warn("영상 전달 실패: episodeId={}, status={}, {}", episodeId, ex.upstreamStatus(), ex.getReason());
            stateService.markUploadFailed(episodeId, token, ex.getReason());

        } catch (Exception ex) {
            log.warn("영상 전달 실패: episodeId={}", episodeId, ex);
            stateService.markUploadFailed(episodeId, token,
                    "영상을 분석 서버로 보내지 못했습니다: " + ex.getClass().getSimpleName());

        } finally {
            if (video != null) {
                try {
                    Files.deleteIfExists(video);
                } catch (IOException ex) {
                    log.warn("임시 MP4 파일 삭제 실패: {}", video, ex);
                }
            }
        }
    }
}

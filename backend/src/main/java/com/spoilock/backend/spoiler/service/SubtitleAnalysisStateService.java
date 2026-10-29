package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbIngestJob;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.repository.EpisodeRepository;
import com.spoilock.backend.spoiler.dto.AdminEpisodeAiResponse;
import com.spoilock.backend.drama.repository.EpisodeVideoRepository;
import com.spoilock.backend.spoiler.entity.EpisodeAnalysis;
import com.spoilock.backend.spoiler.enums.AnalysisSource;
import com.spoilock.backend.spoiler.repository.EpisodeAnalysisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * 자막 분석 상태 저장 (짧은 트랜잭션만).
 * Python 호출은 SubtitleAnalysisService / SubtitleAnalysisPoller 가 트랜잭션 밖에서 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleAnalysisStateService {

    private final EpisodeRepository episodeRepository;
    private final EpisodeAnalysisRepository analysisRepository;
    private final EpisodeVideoRepository videoRepository;

    /** 분석 요청 전 확인 결과 */
    public record Target(
            Long episodeId,
            Long dramaId,
            int episodeNumber,
            String previousJobId,
            AnalysisStatus status,
            boolean requestedBefore,
            // 이전 분석의 원본 (null = V12 이전 자막 분석)
            AnalysisSource previousSource,
            // 영상 분석일 때 저장된 MP4 키 (자막 분석이면 null)
            String videoStorageKey
    ) {
    }

    // ============================================
    // 1. 분석 요청 가능 여부 확인
    // ============================================
    @Transactional(readOnly = true)
    public Target prepare(Long episodeId) {

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회차를 찾을 수 없습니다."
                ));

        EpisodeAnalysis analysis = analysisRepository.findByEpisode_Id(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "자막 업로드가 먼저 필요합니다."
                ));

        if (analysis.getSubtitleFilename() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "등록된 자막이 없습니다."
            );
        }

        if (analysis.isJobActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 분석이 진행 중입니다."
            );
        }

        return toTarget(episode, analysis, null);
    }

    // ============================================
    // 1-1. 영상 분석 요청 가능 여부 확인
    //      자막 없이도 가능 (분석 정보가 없으면 새로 만듦)
    // ============================================
    @Transactional
    public Target prepareVideo(Long episodeId) {

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회차를 찾을 수 없습니다."
                ));

        String storageKey = videoRepository.findByEpisode_Id(episodeId)
                .map(v -> v.getStorageKey())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "등록된 영상이 없습니다. MP4 를 먼저 업로드해 주세요."
                ));

        EpisodeAnalysis analysis = analysisRepository.findByEpisode_Id(episodeId)
                .orElseGet(() -> analysisRepository.saveAndFlush(
                        EpisodeAnalysis.createForVideo(episode)
                ));

        if (analysis.isJobActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 분석이 진행 중입니다."
            );
        }

        return toTarget(episode, analysis, storageKey);
    }

    private static Target toTarget(Episode episode, EpisodeAnalysis analysis, String storageKey) {
        return new Target(
                episode.getId(),
                episode.getDrama().getId(),
                episode.getEpisodeNumber(),
                analysis.getAnalysisJobId(),
                analysis.getAnalysisStatus(),
                analysis.getAnalysisRequestedAt() != null,
                analysis.getAnalysisSource(),
                storageKey
        );
    }

    // ============================================
    // 2. Python 작업 생성 성공 기록
    // ============================================
    @Transactional
    public AdminEpisodeAiResponse markStarted(Long episodeId, KbIngestJob job) {
        return markStarted(episodeId, job.jobId(), AnalysisSource.SUBTITLE);
    }

    @Transactional
    public AdminEpisodeAiResponse markStarted(Long episodeId, String jobId, AnalysisSource source) {

        EpisodeAnalysis analysis = findAnalysis(episodeId);

        analysis.startJob(jobId, source);

        log.info("{} 분석 작업 시작: episodeId={}, jobId={}",
                source == AnalysisSource.VIDEO ? "영상" : "자막", episodeId, jobId);

        return AdminEpisodeAiResponse.from(analysis);
    }

    // ============================================
    // 3. 분석 요청이 거절된 경우 (자막 형식 오류 등)
    // ============================================
    @Transactional
    public void markRequestFailed(Long episodeId, String message) {
        findAnalysis(episodeId).failRequest(message);
    }

    // ============================================
    // 4. 주기적 조회 결과 반영
    //    반환: 이번에 완료된 경우 true (판정 회차 캐시 갱신용)
    // ============================================
    @Transactional
    public boolean applyJob(Long episodeId, String jobId, KbIngestJob job) {

        Optional<EpisodeAnalysis> found = analysisRepository.findByEpisode_Id(episodeId);

        // 그사이 자막이 교체되거나 새 작업이 시작된 경우 → 예전 작업 결과는 무시
        if (found.isEmpty() || !jobId.equals(found.get().getAnalysisJobId())) {
            return false;
        }

        EpisodeAnalysis analysis = found.get();
        AnalysisStatus before = analysis.getAnalysisStatus();
        AnalysisStatus status = toStatus(job.status());

        Integer eventCount = null;
        if (job.isDone()) {
            // 이번에 새로 색인된 사건 + 재분석에서 보존된 검수·수동 사건
            eventCount = (job.eventCount() == null ? 0 : job.eventCount())
                    + (job.keptEvents() == null ? 0 : job.keptEvents());
        }

        analysis.applyJob(
                status,
                job.isDone() ? 100 : job.progressPercent(),
                eventCount,
                job.error()
        );

        if (status != before) {
            log.info("자막 분석 상태 변경: episodeId={}, jobId={}, {} → {}",
                    episodeId, jobId, before, status);
        }

        return status == AnalysisStatus.COMPLETED && before != AnalysisStatus.COMPLETED;
    }

    // ============================================
    // 5. Python 에 작업 기록이 없음 (데이터 폴더 삭제 등)
    // ============================================
    @Transactional
    public void markJobLost(Long episodeId, String jobId) {

        analysisRepository.findByEpisode_Id(episodeId)
                .filter(a -> jobId.equals(a.getAnalysisJobId()))
                .ifPresent(a -> a.applyJob(
                        AnalysisStatus.FAILED,
                        0,
                        null,
                        "분석 서버에서 작업을 찾을 수 없습니다. 다시 분석을 요청해 주세요."
                ));
    }

    private EpisodeAnalysis findAnalysis(Long episodeId) {
        return analysisRepository.findByEpisode_Id(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "자막 분석 정보를 찾을 수 없습니다."
                ));
    }

    /** Python 작업 상태 → Spring 분석 상태 */
    static AnalysisStatus toStatus(String pythonStatus) {
        if (pythonStatus == null) {
            return AnalysisStatus.PROCESSING;
        }
        return switch (pythonStatus) {
            case KbIngestJob.QUEUED -> AnalysisStatus.PENDING;
            case KbIngestJob.DONE -> AnalysisStatus.COMPLETED;
            case KbIngestJob.FAILED -> AnalysisStatus.FAILED;
            default -> AnalysisStatus.PROCESSING;   // PARSING / EXTRACTING / INDEXING
        };
    }
}

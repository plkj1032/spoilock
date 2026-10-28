package com.spoilock.backend.spoiler.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.drama.entity.Episode;
import com.spoilock.backend.spoiler.enums.AnalysisSource;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "episode_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodeAnalysis extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "episode_id",
            nullable = false,
            unique = true
    )
    private Episode episode;

    @Column(
            name = "subtitle_filename",
            length = 255
    )
    private String subtitleFilename;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "analysis_status",
            nullable = false,
            length = 20
    )
    private AnalysisStatus analysisStatus =
            AnalysisStatus.PENDING;

    @Column(
            name = "event_count",
            nullable = false
    )
    private Integer eventCount = 0;

    @Column(
            name = "error_message",
            columnDefinition = "TEXT"
    )
    private String errorMessage;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "analysis_requested_at")
    private LocalDateTime analysisRequestedAt;

    // Python 자막 분석 작업 ID (진행 상태 조회·재시도용)
    @Column(name = "analysis_job_id", length = 64)
    private String analysisJobId;

    // 0 ~ 100 (%)
    @Column(name = "analysis_progress")
    private Integer analysisProgress;

    // 분석 원본 (자막 / 영상). 진행 상태 조회 시 Python 작업 종류 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_source", length = 20)
    private AnalysisSource analysisSource;

    public static EpisodeAnalysis create(
            Episode episode,
            String filename
    )
    {
        EpisodeAnalysis analysis = new EpisodeAnalysis();

        analysis.episode = episode;
        analysis.subtitleFilename = filename;
        analysis.analysisStatus = AnalysisStatus.PENDING;

        return analysis;
    }

    /** 자막 없이 영상으로 분석하는 회차 */
    public static EpisodeAnalysis createForVideo(Episode episode)
    {
        EpisodeAnalysis analysis = new EpisodeAnalysis();

        analysis.episode = episode;
        analysis.analysisStatus = AnalysisStatus.PENDING;

        return analysis;
    }

    public void attachSubtitle(String filename)
    {
        if(this.subtitleFilename != null)
        {
            throw new IllegalStateException(
                    "이미 자막이 등록되어 있습니다."
            );
        }

        this.subtitleFilename = filename;
        this.analysisStatus = AnalysisStatus.PENDING;
    }

    /** Python 분석 작업이 대기·진행 중인지 */
    public boolean isJobActive()
    {
        return this.analysisJobId != null
                && this.analysisRequestedAt != null
                && (this.analysisStatus == AnalysisStatus.PENDING
                || this.analysisStatus == AnalysisStatus.PROCESSING);
    }

    /** Python 분석 작업 시작(또는 재시도) 기록 */
    public void startJob(String jobId, AnalysisSource source)
    {
        this.analysisSource = source;
        this.analysisJobId = jobId;
        this.analysisRequestedAt = LocalDateTime.now();
        this.analysisStatus = AnalysisStatus.PENDING;
        this.analysisProgress = 0;
        this.errorMessage = null;
    }

    /** Python 작업 상태 반영 (주기적 조회 결과) */
    public void applyJob(
            AnalysisStatus status,
            int progress,
            Integer eventCount,
            String error
    )
    {
        this.analysisStatus = status;
        this.analysisProgress = progress;

        if (status == AnalysisStatus.COMPLETED) {
            this.analyzedAt = LocalDateTime.now();
            this.errorMessage = null;
            if (eventCount != null) {
                this.eventCount = eventCount;
            }
        } else if (status == AnalysisStatus.FAILED) {
            this.errorMessage = error;
        }
    }

    /** 분석 요청 자체가 거절된 경우 (자막 형식 오류 등) */
    public void failRequest(String message)
    {
        this.analysisRequestedAt = LocalDateTime.now();
        this.analysisStatus = AnalysisStatus.FAILED;
        this.analysisProgress = null;
        this.errorMessage = message;
    }

    /**
     * 자막 삭제 (교체 업로드 준비).
     * 이전 작업 ID 를 지워, 새 자막으로 분석할 때 예전 작업을 재시도하지 않게 한다.
     * Python 에 이미 등록된 사건은 남아 있으므로 새 자막 분석은 재분석(replace)으로 요청해야 한다.
     */
    public void detachSubtitle()
    {
        if (isJobActive()) {
            throw new IllegalStateException("분석 작업이 진행 중입니다.");
        }
        this.subtitleFilename = null;
        this.analysisJobId = null;
        this.analysisSource = null;
        this.analysisProgress = null;
        this.analysisRequestedAt = null;
        this.analysisStatus = AnalysisStatus.PENDING;
        this.errorMessage = null;
    }
}

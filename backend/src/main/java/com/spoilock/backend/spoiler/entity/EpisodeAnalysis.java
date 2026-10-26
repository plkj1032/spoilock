package com.spoilock.backend.spoiler.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.drama.entity.Episode;

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

    public void requestAnalysis()
    {
        this.analysisRequestedAt = LocalDateTime.now();
        this.analysisStatus = AnalysisStatus.PENDING;
        this.errorMessage = null;
    }
}

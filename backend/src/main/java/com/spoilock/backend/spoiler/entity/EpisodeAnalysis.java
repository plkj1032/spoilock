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
}

package com.spoilock.backend.spoiler.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.drama.entity.Episode;
import com.spoilock.backend.spoiler.enums.SpoilerSourceType;
import com.spoilock.backend.spoiler.enums.IndexStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "spoiler_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpoilerEvent extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "episode_id",
            nullable = false
    )
    private Episode episode;

    @Column(
            name = "chroma_doc_id",
            unique = true,
            length = 255
    )
    private String chromaDocId;

    @Column(
            name = "event_summary",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String eventSummary;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source_type",
            nullable = false,
            length = 30
    )
    private SpoilerSourceType sourceType;

    @Column(nullable = false)
    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "index_status",
            nullable = false,
            length = 20
    )
    private IndexStatus indexStatus =
            IndexStatus.PENDING;

    public static SpoilerEvent createManual(
            Episode episode,
            String eventSummary
    )
    {
        SpoilerEvent event = new SpoilerEvent();

        event.episode = episode;
        event.eventSummary = eventSummary;
        event.sourceType = SpoilerSourceType.MANUAL;
        event.verified = false;
        event.indexStatus = IndexStatus.PENDING;

        return event;
    }

    public void review(boolean verified)
    {
        this.verified = verified;
    }
}

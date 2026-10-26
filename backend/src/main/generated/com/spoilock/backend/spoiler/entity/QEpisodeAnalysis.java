package com.spoilock.backend.spoiler.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEpisodeAnalysis is a Querydsl query type for EpisodeAnalysis
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEpisodeAnalysis extends EntityPathBase<EpisodeAnalysis> {

    private static final long serialVersionUID = -2065448847L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEpisodeAnalysis episodeAnalysis = new QEpisodeAnalysis("episodeAnalysis");

    public final com.spoilock.backend.common.entity.QAuditedEntity _super = new com.spoilock.backend.common.entity.QAuditedEntity(this);

    public final DateTimePath<java.time.LocalDateTime> analysisRequestedAt = createDateTime("analysisRequestedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.spoilock.backend.common.enums.AnalysisStatus> analysisStatus = createEnum("analysisStatus", com.spoilock.backend.common.enums.AnalysisStatus.class);

    public final DateTimePath<java.time.LocalDateTime> analyzedAt = createDateTime("analyzedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.spoilock.backend.drama.entity.QEpisode episode;

    public final StringPath errorMessage = createString("errorMessage");

    public final NumberPath<Integer> eventCount = createNumber("eventCount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath subtitleFilename = createString("subtitleFilename");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QEpisodeAnalysis(String variable) {
        this(EpisodeAnalysis.class, forVariable(variable), INITS);
    }

    public QEpisodeAnalysis(Path<? extends EpisodeAnalysis> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEpisodeAnalysis(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEpisodeAnalysis(PathMetadata metadata, PathInits inits) {
        this(EpisodeAnalysis.class, metadata, inits);
    }

    public QEpisodeAnalysis(Class<? extends EpisodeAnalysis> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.episode = inits.isInitialized("episode") ? new com.spoilock.backend.drama.entity.QEpisode(forProperty("episode"), inits.get("episode")) : null;
    }

}


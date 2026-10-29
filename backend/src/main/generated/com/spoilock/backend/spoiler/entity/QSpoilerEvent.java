package com.spoilock.backend.spoiler.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpoilerEvent is a Querydsl query type for SpoilerEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpoilerEvent extends EntityPathBase<SpoilerEvent> {

    private static final long serialVersionUID = 802416062L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpoilerEvent spoilerEvent = new QSpoilerEvent("spoilerEvent");

    public final com.spoilock.backend.common.entity.QAuditedEntity _super = new com.spoilock.backend.common.entity.QAuditedEntity(this);

    public final StringPath chromaDocId = createString("chromaDocId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.spoilock.backend.drama.domain.entity.QEpisode episode;

    public final StringPath eventSummary = createString("eventSummary");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.spoilock.backend.spoiler.enums.IndexStatus> indexStatus = createEnum("indexStatus", com.spoilock.backend.spoiler.enums.IndexStatus.class);

    public final EnumPath<com.spoilock.backend.spoiler.enums.SpoilerSourceType> sourceType = createEnum("sourceType", com.spoilock.backend.spoiler.enums.SpoilerSourceType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final BooleanPath verified = createBoolean("verified");

    public QSpoilerEvent(String variable) {
        this(SpoilerEvent.class, forVariable(variable), INITS);
    }

    public QSpoilerEvent(Path<? extends SpoilerEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpoilerEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpoilerEvent(PathMetadata metadata, PathInits inits) {
        this(SpoilerEvent.class, metadata, inits);
    }

    public QSpoilerEvent(Class<? extends SpoilerEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.episode = inits.isInitialized("episode") ? new com.spoilock.backend.drama.domain.entity.QEpisode(forProperty("episode"), inits.get("episode")) : null;
    }

}


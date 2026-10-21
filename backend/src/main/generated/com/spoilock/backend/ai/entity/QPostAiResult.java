package com.spoilock.backend.ai.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostAiResult is a Querydsl query type for PostAiResult
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostAiResult extends EntityPathBase<PostAiResult> {

    private static final long serialVersionUID = 1270881955L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostAiResult postAiResult = new QPostAiResult("postAiResult");

    public final DateTimePath<java.time.LocalDateTime> analyzedAt = createDateTime("analyzedAt", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> confidenceScore = createNumber("confidenceScore", java.math.BigDecimal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.spoilock.backend.spoiler.entity.QSpoilerEvent matchedEvent;

    public final com.spoilock.backend.post.entity.QPost post;

    public final NumberPath<Integer> predictedEpisodeNumber = createNumber("predictedEpisodeNumber", Integer.class);

    public final StringPath reason = createString("reason");

    public final EnumPath<com.spoilock.backend.ai.enums.RiskLevel> riskLevel = createEnum("riskLevel", com.spoilock.backend.ai.enums.RiskLevel.class);

    public QPostAiResult(String variable) {
        this(PostAiResult.class, forVariable(variable), INITS);
    }

    public QPostAiResult(Path<? extends PostAiResult> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostAiResult(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostAiResult(PathMetadata metadata, PathInits inits) {
        this(PostAiResult.class, metadata, inits);
    }

    public QPostAiResult(Class<? extends PostAiResult> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.matchedEvent = inits.isInitialized("matchedEvent") ? new com.spoilock.backend.spoiler.entity.QSpoilerEvent(forProperty("matchedEvent"), inits.get("matchedEvent")) : null;
        this.post = inits.isInitialized("post") ? new com.spoilock.backend.post.entity.QPost(forProperty("post"), inits.get("post")) : null;
    }

}


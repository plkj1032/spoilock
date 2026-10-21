package com.spoilock.backend.ai.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatAiResult is a Querydsl query type for ChatAiResult
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatAiResult extends EntityPathBase<ChatAiResult> {

    private static final long serialVersionUID = -1471159973L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatAiResult chatAiResult = new QChatAiResult("chatAiResult");

    public final DateTimePath<java.time.LocalDateTime> analyzedAt = createDateTime("analyzedAt", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> confidenceScore = createNumber("confidenceScore", java.math.BigDecimal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.spoilock.backend.spoiler.entity.QSpoilerEvent matchedEvent;

    public final com.spoilock.backend.chat.domain.entity.QChatMessage message;

    public final NumberPath<Integer> predictedEpisodeNumber = createNumber("predictedEpisodeNumber", Integer.class);

    public final StringPath reason = createString("reason");

    public final EnumPath<com.spoilock.backend.ai.enums.RiskLevel> riskLevel = createEnum("riskLevel", com.spoilock.backend.ai.enums.RiskLevel.class);

    public QChatAiResult(String variable) {
        this(ChatAiResult.class, forVariable(variable), INITS);
    }

    public QChatAiResult(Path<? extends ChatAiResult> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatAiResult(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatAiResult(PathMetadata metadata, PathInits inits) {
        this(ChatAiResult.class, metadata, inits);
    }

    public QChatAiResult(Class<? extends ChatAiResult> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.matchedEvent = inits.isInitialized("matchedEvent") ? new com.spoilock.backend.spoiler.entity.QSpoilerEvent(forProperty("matchedEvent"), inits.get("matchedEvent")) : null;
        this.message = inits.isInitialized("message") ? new com.spoilock.backend.chat.domain.entity.QChatMessage(forProperty("message"), inits.get("message")) : null;
    }

}


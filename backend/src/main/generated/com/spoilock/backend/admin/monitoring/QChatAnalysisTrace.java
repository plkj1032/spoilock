package com.spoilock.backend.admin.monitoring;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatAnalysisTrace is a Querydsl query type for ChatAnalysisTrace
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatAnalysisTrace extends EntityPathBase<ChatAnalysisTrace> {

    private static final long serialVersionUID = -127120957L;

    public static final QChatAnalysisTrace chatAnalysisTrace = new QChatAnalysisTrace("chatAnalysisTrace");

    public final NumberPath<Long> durationMs = createNumber("durationMs", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> messageId = createNumber("messageId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> recordedAt = createDateTime("recordedAt", java.time.LocalDateTime.class);

    public final StringPath stage = createString("stage");

    public QChatAnalysisTrace(String variable) {
        super(ChatAnalysisTrace.class, forVariable(variable));
    }

    public QChatAnalysisTrace(Path<? extends ChatAnalysisTrace> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatAnalysisTrace(PathMetadata metadata) {
        super(ChatAnalysisTrace.class, metadata);
    }

}


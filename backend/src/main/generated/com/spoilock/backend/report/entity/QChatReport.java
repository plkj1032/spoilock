package com.spoilock.backend.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatReport is a Querydsl query type for ChatReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatReport extends EntityPathBase<ChatReport> {

    private static final long serialVersionUID = 223367990L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatReport chatReport = new QChatReport("chatReport");

    public final com.spoilock.backend.common.entity.QCreatedAtEntity _super = new com.spoilock.backend.common.entity.QCreatedAtEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.spoilock.backend.chat.domain.entity.QChatMessage message;

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.spoilock.backend.report.enums.ReportReason> reason = createEnum("reason", com.spoilock.backend.report.enums.ReportReason.class);

    public final com.spoilock.backend.user.entity.QUser reporter;

    public final EnumPath<com.spoilock.backend.report.enums.ReportStatus> status = createEnum("status", com.spoilock.backend.report.enums.ReportStatus.class);

    public QChatReport(String variable) {
        this(ChatReport.class, forVariable(variable), INITS);
    }

    public QChatReport(Path<? extends ChatReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatReport(PathMetadata metadata, PathInits inits) {
        this(ChatReport.class, metadata, inits);
    }

    public QChatReport(Class<? extends ChatReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.message = inits.isInitialized("message") ? new com.spoilock.backend.chat.domain.entity.QChatMessage(forProperty("message"), inits.get("message")) : null;
        this.reporter = inits.isInitialized("reporter") ? new com.spoilock.backend.user.entity.QUser(forProperty("reporter")) : null;
    }

}


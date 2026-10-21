package com.spoilock.backend.report.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostReport is a Querydsl query type for PostReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostReport extends EntityPathBase<PostReport> {

    private static final long serialVersionUID = -792771970L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostReport postReport = new QPostReport("postReport");

    public final com.spoilock.backend.common.entity.QCreatedAtEntity _super = new com.spoilock.backend.common.entity.QCreatedAtEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.spoilock.backend.post.entity.QPost post;

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.spoilock.backend.report.enums.ReportReason> reason = createEnum("reason", com.spoilock.backend.report.enums.ReportReason.class);

    public final com.spoilock.backend.user.entity.QUser reporter;

    public final EnumPath<com.spoilock.backend.report.enums.ReportStatus> status = createEnum("status", com.spoilock.backend.report.enums.ReportStatus.class);

    public QPostReport(String variable) {
        this(PostReport.class, forVariable(variable), INITS);
    }

    public QPostReport(Path<? extends PostReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostReport(PathMetadata metadata, PathInits inits) {
        this(PostReport.class, metadata, inits);
    }

    public QPostReport(Class<? extends PostReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new com.spoilock.backend.post.entity.QPost(forProperty("post"), inits.get("post")) : null;
        this.reporter = inits.isInitialized("reporter") ? new com.spoilock.backend.user.entity.QUser(forProperty("reporter")) : null;
    }

}


package com.spoilock.backend.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTermsDocument is a Querydsl query type for TermsDocument
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTermsDocument extends EntityPathBase<TermsDocument> {

    private static final long serialVersionUID = -674832767L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTermsDocument termsDocument = new QTermsDocument("termsDocument");

    public final com.spoilock.backend.common.entity.QAuditedEntity _super = new com.spoilock.backend.common.entity.QAuditedEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QUser createdBy;

    public final DateTimePath<java.time.LocalDateTime> effectiveAt = createDateTime("effectiveAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> publishedAt = createDateTime("publishedAt", java.time.LocalDateTime.class);

    public final BooleanPath required = createBoolean("required");

    public final StringPath status = createString("status");

    public final StringPath termsType = createString("termsType");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath version = createString("version");

    public QTermsDocument(String variable) {
        this(TermsDocument.class, forVariable(variable), INITS);
    }

    public QTermsDocument(Path<? extends TermsDocument> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTermsDocument(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTermsDocument(PathMetadata metadata, PathInits inits) {
        this(TermsDocument.class, metadata, inits);
    }

    public QTermsDocument(Class<? extends TermsDocument> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.createdBy = inits.isInitialized("createdBy") ? new QUser(forProperty("createdBy")) : null;
    }

}


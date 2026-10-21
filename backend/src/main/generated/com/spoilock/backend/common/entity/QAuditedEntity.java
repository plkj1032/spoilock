package com.spoilock.backend.common.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAuditedEntity is a Querydsl query type for AuditedEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QAuditedEntity extends EntityPathBase<AuditedEntity> {

    private static final long serialVersionUID = 937732380L;

    public static final QAuditedEntity auditedEntity = new QAuditedEntity("auditedEntity");

    public final QCreatedAtEntity _super = new QCreatedAtEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QAuditedEntity(String variable) {
        super(AuditedEntity.class, forVariable(variable));
    }

    public QAuditedEntity(Path<? extends AuditedEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAuditedEntity(PathMetadata metadata) {
        super(AuditedEntity.class, metadata);
    }

}


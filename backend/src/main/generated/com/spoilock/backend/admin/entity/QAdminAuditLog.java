package com.spoilock.backend.admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAdminAuditLog is a Querydsl query type for AdminAuditLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminAuditLog extends EntityPathBase<AdminAuditLog> {

    private static final long serialVersionUID = -1716542043L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAdminAuditLog adminAuditLog = new QAdminAuditLog("adminAuditLog");

    public final com.spoilock.backend.common.entity.QCreatedAtEntity _super = new com.spoilock.backend.common.entity.QCreatedAtEntity(this);

    public final EnumPath<com.spoilock.backend.admin.enums.AdminAction> action = createEnum("action", com.spoilock.backend.admin.enums.AdminAction.class);

    public final com.spoilock.backend.user.entity.QUser adminUser;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final EnumPath<com.spoilock.backend.admin.enums.AdminTargetType> targetType = createEnum("targetType", com.spoilock.backend.admin.enums.AdminTargetType.class);

    public QAdminAuditLog(String variable) {
        this(AdminAuditLog.class, forVariable(variable), INITS);
    }

    public QAdminAuditLog(Path<? extends AdminAuditLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAdminAuditLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAdminAuditLog(PathMetadata metadata, PathInits inits) {
        this(AdminAuditLog.class, metadata, inits);
    }

    public QAdminAuditLog(Class<? extends AdminAuditLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.adminUser = inits.isInitialized("adminUser") ? new com.spoilock.backend.user.entity.QUser(forProperty("adminUser")) : null;
    }

}


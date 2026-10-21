package com.spoilock.backend.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRefreshSession is a Querydsl query type for RefreshSession
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRefreshSession extends EntityPathBase<RefreshSession> {

    private static final long serialVersionUID = -2054509476L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRefreshSession refreshSession = new QRefreshSession("refreshSession");

    public final com.spoilock.backend.common.entity.QCreatedAtEntity _super = new com.spoilock.backend.common.entity.QCreatedAtEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> revokedAt = createDateTime("revokedAt", java.time.LocalDateTime.class);

    public final StringPath tokenHash = createString("tokenHash");

    public final QUser user;

    public QRefreshSession(String variable) {
        this(RefreshSession.class, forVariable(variable), INITS);
    }

    public QRefreshSession(Path<? extends RefreshSession> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRefreshSession(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRefreshSession(PathMetadata metadata, PathInits inits) {
        this(RefreshSession.class, metadata, inits);
    }

    public QRefreshSession(Class<? extends RefreshSession> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}


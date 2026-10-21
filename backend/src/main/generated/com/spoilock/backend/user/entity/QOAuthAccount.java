package com.spoilock.backend.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOAuthAccount is a Querydsl query type for OAuthAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOAuthAccount extends EntityPathBase<OAuthAccount> {

    private static final long serialVersionUID = 340543703L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOAuthAccount oAuthAccount = new QOAuthAccount("oAuthAccount");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> linkedAt = createDateTime("linkedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.spoilock.backend.user.enums.OAuthProvider> provider = createEnum("provider", com.spoilock.backend.user.enums.OAuthProvider.class);

    public final StringPath providerUserId = createString("providerUserId");

    public final QUser user;

    public QOAuthAccount(String variable) {
        this(OAuthAccount.class, forVariable(variable), INITS);
    }

    public QOAuthAccount(Path<? extends OAuthAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOAuthAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOAuthAccount(PathMetadata metadata, PathInits inits) {
        this(OAuthAccount.class, metadata, inits);
    }

    public QOAuthAccount(Class<? extends OAuthAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}


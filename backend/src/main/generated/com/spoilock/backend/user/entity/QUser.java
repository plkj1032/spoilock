package com.spoilock.backend.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 562672172L;

    public static final QUser user = new QUser("user");

    public final com.spoilock.backend.common.entity.QAuditedEntity _super = new com.spoilock.backend.common.entity.QAuditedEntity(this);

    public final EnumPath<com.spoilock.backend.user.enums.AccountStatus> accountStatus = createEnum("accountStatus", com.spoilock.backend.user.enums.AccountStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final DateTimePath<java.time.LocalDateTime> emailVerifiedAt = createDateTime("emailVerifiedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath nickname = createString("nickname");

    public final StringPath nicknameKey = createString("nicknameKey");

    public final StringPath passwordHash = createString("passwordHash");

    public final EnumPath<com.spoilock.backend.user.enums.UserRole> role = createEnum("role", com.spoilock.backend.user.enums.UserRole.class);

    public final NumberPath<Long> tokenVersion = createNumber("tokenVersion", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}


package com.spoilock.backend.drama.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDramaRating is a Querydsl query type for DramaRating
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDramaRating extends EntityPathBase<DramaRating> {

    private static final long serialVersionUID = 1526731779L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDramaRating dramaRating = new QDramaRating("dramaRating");

    public final com.spoilock.backend.common.entity.QAuditedEntity _super = new com.spoilock.backend.common.entity.QAuditedEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QDrama drama;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.spoilock.backend.user.entity.QUser user;

    public QDramaRating(String variable) {
        this(DramaRating.class, forVariable(variable), INITS);
    }

    public QDramaRating(Path<? extends DramaRating> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDramaRating(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDramaRating(PathMetadata metadata, PathInits inits) {
        this(DramaRating.class, metadata, inits);
    }

    public QDramaRating(Class<? extends DramaRating> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.drama = inits.isInitialized("drama") ? new QDrama(forProperty("drama")) : null;
        this.user = inits.isInitialized("user") ? new com.spoilock.backend.user.entity.QUser(forProperty("user")) : null;
    }

}


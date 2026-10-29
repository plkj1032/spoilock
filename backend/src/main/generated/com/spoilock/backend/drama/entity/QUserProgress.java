package com.spoilock.backend.drama.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserProgress is a Querydsl query type for UserProgress
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserProgress extends EntityPathBase<UserProgress> {

    private static final long serialVersionUID = 1894819011L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserProgress userProgress = new QUserProgress("userProgress");

    public final QDrama drama;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final com.spoilock.backend.user.entity.QUser user;

    public final NumberPath<Integer> watchedEpisodeNumber = createNumber("watchedEpisodeNumber", Integer.class);

    public QUserProgress(String variable) {
        this(UserProgress.class, forVariable(variable), INITS);
    }

    public QUserProgress(Path<? extends UserProgress> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserProgress(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserProgress(PathMetadata metadata, PathInits inits) {
        this(UserProgress.class, metadata, inits);
    }

    public QUserProgress(Class<? extends UserProgress> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.drama = inits.isInitialized("drama") ? new QDrama(forProperty("drama")) : null;
        this.user = inits.isInitialized("user") ? new com.spoilock.backend.user.entity.QUser(forProperty("user")) : null;
    }

}


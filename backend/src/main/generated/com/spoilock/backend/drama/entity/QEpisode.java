package com.spoilock.backend.drama.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEpisode is a Querydsl query type for Episode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEpisode extends EntityPathBase<Episode> {

    private static final long serialVersionUID = -1609363088L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEpisode episode = new QEpisode("episode");

    public final com.spoilock.backend.common.entity.QCreatedAtEntity _super = new com.spoilock.backend.common.entity.QCreatedAtEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QDrama drama;

    public final NumberPath<Integer> episodeNumber = createNumber("episodeNumber", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> releasedAt = createDateTime("releasedAt", java.time.LocalDateTime.class);

    public final StringPath title = createString("title");

    public QEpisode(String variable) {
        this(Episode.class, forVariable(variable), INITS);
    }

    public QEpisode(Path<? extends Episode> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEpisode(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEpisode(PathMetadata metadata, PathInits inits) {
        this(Episode.class, metadata, inits);
    }

    public QEpisode(Class<? extends Episode> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.drama = inits.isInitialized("drama") ? new QDrama(forProperty("drama")) : null;
    }

}


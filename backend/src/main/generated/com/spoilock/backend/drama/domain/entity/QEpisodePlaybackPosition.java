package com.spoilock.backend.drama.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEpisodePlaybackPosition is a Querydsl query type for EpisodePlaybackPosition
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEpisodePlaybackPosition extends EntityPathBase<EpisodePlaybackPosition> {

    private static final long serialVersionUID = -417848546L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEpisodePlaybackPosition episodePlaybackPosition = new QEpisodePlaybackPosition("episodePlaybackPosition");

    public final NumberPath<Double> durationSeconds = createNumber("durationSeconds", Double.class);

    public final QEpisode episode;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Double> positionSeconds = createNumber("positionSeconds", Double.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final com.spoilock.backend.user.entity.QUser user;

    public QEpisodePlaybackPosition(String variable) {
        this(EpisodePlaybackPosition.class, forVariable(variable), INITS);
    }

    public QEpisodePlaybackPosition(Path<? extends EpisodePlaybackPosition> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEpisodePlaybackPosition(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEpisodePlaybackPosition(PathMetadata metadata, PathInits inits) {
        this(EpisodePlaybackPosition.class, metadata, inits);
    }

    public QEpisodePlaybackPosition(Class<? extends EpisodePlaybackPosition> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.episode = inits.isInitialized("episode") ? new QEpisode(forProperty("episode"), inits.get("episode")) : null;
        this.user = inits.isInitialized("user") ? new com.spoilock.backend.user.entity.QUser(forProperty("user")) : null;
    }

}


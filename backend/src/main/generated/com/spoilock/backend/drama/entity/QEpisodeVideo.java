package com.spoilock.backend.drama.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEpisodeVideo is a Querydsl query type for EpisodeVideo
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEpisodeVideo extends EntityPathBase<EpisodeVideo> {

    private static final long serialVersionUID = 976976107L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEpisodeVideo episodeVideo = new QEpisodeVideo("episodeVideo");

    public final QEpisode episode;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath originalFilename = createString("originalFilename");

    public final NumberPath<Long> sizeBytes = createNumber("sizeBytes", Long.class);

    public final StringPath storageKey = createString("storageKey");

    public final DateTimePath<java.time.LocalDateTime> uploadedAt = createDateTime("uploadedAt", java.time.LocalDateTime.class);

    public QEpisodeVideo(String variable) {
        this(EpisodeVideo.class, forVariable(variable), INITS);
    }

    public QEpisodeVideo(Path<? extends EpisodeVideo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEpisodeVideo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEpisodeVideo(PathMetadata metadata, PathInits inits) {
        this(EpisodeVideo.class, metadata, inits);
    }

    public QEpisodeVideo(Class<? extends EpisodeVideo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.episode = inits.isInitialized("episode") ? new QEpisode(forProperty("episode"), inits.get("episode")) : null;
    }

}


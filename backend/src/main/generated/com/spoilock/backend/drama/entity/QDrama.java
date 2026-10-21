package com.spoilock.backend.drama.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDrama is a Querydsl query type for Drama
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDrama extends EntityPathBase<Drama> {

    private static final long serialVersionUID = -1852823780L;

    public static final QDrama drama = new QDrama("drama");

    public final com.spoilock.backend.common.entity.QAuditedEntity _super = new com.spoilock.backend.common.entity.QAuditedEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath posterUrl = createString("posterUrl");

    public final EnumPath<com.spoilock.backend.drama.enums.DramaStatus> status = createEnum("status", com.spoilock.backend.drama.enums.DramaStatus.class);

    public final StringPath synopsis = createString("synopsis");

    public final StringPath title = createString("title");

    public final NumberPath<Integer> totalEpisodeCount = createNumber("totalEpisodeCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QDrama(String variable) {
        super(Drama.class, forVariable(variable));
    }

    public QDrama(Path<? extends Drama> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDrama(PathMetadata metadata) {
        super(Drama.class, metadata);
    }

}


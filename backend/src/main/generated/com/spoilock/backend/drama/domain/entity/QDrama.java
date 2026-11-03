package com.spoilock.backend.drama.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDrama is a Querydsl query type for Drama
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDrama extends EntityPathBase<Drama> {

    private static final long serialVersionUID = -548068666L;

    public static final QDrama drama = new QDrama("drama");

    public final com.spoilock.backend.common.entity.QAuditedEntity _super = new com.spoilock.backend.common.entity.QAuditedEntity(this);

    public final StringPath castNames = createString("castNames");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final StringPath directors = createString("directors");

    public final SetPath<com.spoilock.backend.drama.domain.enums.DramaGenre, EnumPath<com.spoilock.backend.drama.domain.enums.DramaGenre>> genres = this.<com.spoilock.backend.drama.domain.enums.DramaGenre, EnumPath<com.spoilock.backend.drama.domain.enums.DramaGenre>>createSet("genres", com.spoilock.backend.drama.domain.enums.DramaGenre.class, EnumPath.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath officialHomepageUrl = createString("officialHomepageUrl");

    public final StringPath posterUrl = createString("posterUrl");

    public final DatePath<java.time.LocalDate> premiereDate = createDate("premiereDate", java.time.LocalDate.class);

    public final EnumPath<com.spoilock.backend.drama.domain.enums.DramaProvisionType> provisionType = createEnum("provisionType", com.spoilock.backend.drama.domain.enums.DramaProvisionType.class);

    public final EnumPath<com.spoilock.backend.drama.domain.enums.DramaStatus> status = createEnum("status", com.spoilock.backend.drama.domain.enums.DramaStatus.class);

    public final StringPath synopsis = createString("synopsis");

    public final StringPath title = createString("title");

    public final NumberPath<Integer> totalEpisodeCount = createNumber("totalEpisodeCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath writers = createString("writers");

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


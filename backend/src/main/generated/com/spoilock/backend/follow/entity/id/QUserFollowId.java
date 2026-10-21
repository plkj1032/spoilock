package com.spoilock.backend.follow.entity.id;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserFollowId is a Querydsl query type for UserFollowId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QUserFollowId extends BeanPath<UserFollowId> {

    private static final long serialVersionUID = 626922083L;

    public static final QUserFollowId userFollowId = new QUserFollowId("userFollowId");

    public final NumberPath<Long> followerId = createNumber("followerId", Long.class);

    public final NumberPath<Long> followingId = createNumber("followingId", Long.class);

    public QUserFollowId(String variable) {
        super(UserFollowId.class, forVariable(variable));
    }

    public QUserFollowId(Path<? extends UserFollowId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserFollowId(PathMetadata metadata) {
        super(UserFollowId.class, metadata);
    }

}


package com.spoilock.backend.post.entity.id;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPostLikeId is a Querydsl query type for PostLikeId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QPostLikeId extends BeanPath<PostLikeId> {

    private static final long serialVersionUID = 1617476559L;

    public static final QPostLikeId postLikeId = new QPostLikeId("postLikeId");

    public final NumberPath<Long> postId = createNumber("postId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPostLikeId(String variable) {
        super(PostLikeId.class, forVariable(variable));
    }

    public QPostLikeId(Path<? extends PostLikeId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPostLikeId(PathMetadata metadata) {
        super(PostLikeId.class, metadata);
    }

}


package com.spoilock.backend.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEmailVerification is a Querydsl query type for EmailVerification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEmailVerification extends EntityPathBase<EmailVerification> {

    private static final long serialVersionUID = 297487126L;

    public static final QEmailVerification emailVerification = new QEmailVerification("emailVerification");

    public final NumberPath<Integer> attemptCount = createNumber("attemptCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> codeExpiresAt = createDateTime("codeExpiresAt", java.time.LocalDateTime.class);

    public final StringPath codeHash = createString("codeHash");

    public final DateTimePath<java.time.LocalDateTime> consumedAt = createDateTime("consumedAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastSentAt = createDateTime("lastSentAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> proofExpiresAt = createDateTime("proofExpiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> sendCount = createNumber("sendCount", Integer.class);

    public final StringPath verificationTokenHash = createString("verificationTokenHash");

    public final DateTimePath<java.time.LocalDateTime> verifiedAt = createDateTime("verifiedAt", java.time.LocalDateTime.class);

    public QEmailVerification(String variable) {
        super(EmailVerification.class, forVariable(variable));
    }

    public QEmailVerification(Path<? extends EmailVerification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEmailVerification(PathMetadata metadata) {
        super(EmailVerification.class, metadata);
    }

}


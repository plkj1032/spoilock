package com.spoilock.backend.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserTermsAcceptance is a Querydsl query type for UserTermsAcceptance
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserTermsAcceptance extends EntityPathBase<UserTermsAcceptance> {

    private static final long serialVersionUID = -761543822L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserTermsAcceptance userTermsAcceptance = new QUserTermsAcceptance("userTermsAcceptance");

    public final StringPath acceptanceSource = createString("acceptanceSource");

    public final DateTimePath<java.time.LocalDateTime> acceptedAt = createDateTime("acceptedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QTermsDocument termsDocument;

    public final QUser user;

    public QUserTermsAcceptance(String variable) {
        this(UserTermsAcceptance.class, forVariable(variable), INITS);
    }

    public QUserTermsAcceptance(Path<? extends UserTermsAcceptance> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserTermsAcceptance(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserTermsAcceptance(PathMetadata metadata, PathInits inits) {
        this(UserTermsAcceptance.class, metadata, inits);
    }

    public QUserTermsAcceptance(Class<? extends UserTermsAcceptance> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.termsDocument = inits.isInitialized("termsDocument") ? new QTermsDocument(forProperty("termsDocument"), inits.get("termsDocument")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}


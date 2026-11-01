package com.spoilock.backend.chat.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatInviteLink is a Querydsl query type for ChatInviteLink
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatInviteLink extends EntityPathBase<ChatInviteLink> {

    private static final long serialVersionUID = 607936313L;

    public static final QChatInviteLink chatInviteLink = new QChatInviteLink("chatInviteLink");

    public final StringPath codeHash = createString("codeHash");

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> issuerId = createNumber("issuerId", Long.class);

    public final NumberPath<Integer> maxUses = createNumber("maxUses", Integer.class);

    public final BooleanPath revoked = createBoolean("revoked");

    public final NumberPath<Long> roomId = createNumber("roomId", Long.class);

    public final NumberPath<Integer> usedCount = createNumber("usedCount", Integer.class);

    public QChatInviteLink(String variable) {
        super(ChatInviteLink.class, forVariable(variable));
    }

    public QChatInviteLink(Path<? extends ChatInviteLink> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatInviteLink(PathMetadata metadata) {
        super(ChatInviteLink.class, metadata);
    }

}


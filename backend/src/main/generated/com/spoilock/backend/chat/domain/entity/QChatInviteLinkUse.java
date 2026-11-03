package com.spoilock.backend.chat.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatInviteLinkUse is a Querydsl query type for ChatInviteLinkUse
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatInviteLinkUse extends EntityPathBase<ChatInviteLinkUse> {

    private static final long serialVersionUID = -846301298L;

    public static final QChatInviteLinkUse chatInviteLinkUse = new QChatInviteLinkUse("chatInviteLinkUse");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> linkId = createNumber("linkId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QChatInviteLinkUse(String variable) {
        super(ChatInviteLinkUse.class, forVariable(variable));
    }

    public QChatInviteLinkUse(Path<? extends ChatInviteLinkUse> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatInviteLinkUse(PathMetadata metadata) {
        super(ChatInviteLinkUse.class, metadata);
    }

}


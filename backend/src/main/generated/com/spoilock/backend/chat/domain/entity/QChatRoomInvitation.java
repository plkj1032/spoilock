package com.spoilock.backend.chat.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatRoomInvitation is a Querydsl query type for ChatRoomInvitation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomInvitation extends EntityPathBase<ChatRoomInvitation> {

    private static final long serialVersionUID = 604519530L;

    public static final QChatRoomInvitation chatRoomInvitation = new QChatRoomInvitation("chatRoomInvitation");

    public final com.spoilock.backend.common.entity.QCreatedAtEntity _super = new com.spoilock.backend.common.entity.QCreatedAtEntity(this);

    public final StringPath codeHash = createString("codeHash");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> receiverUserId = createNumber("receiverUserId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> respondedAt = createDateTime("respondedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> roomId = createNumber("roomId", Long.class);

    public final NumberPath<Long> senderUserId = createNumber("senderUserId", Long.class);

    public final EnumPath<com.spoilock.backend.chat.domain.type.ChatInvitationStatus> status = createEnum("status", com.spoilock.backend.chat.domain.type.ChatInvitationStatus.class);

    public QChatRoomInvitation(String variable) {
        super(ChatRoomInvitation.class, forVariable(variable));
    }

    public QChatRoomInvitation(Path<? extends ChatRoomInvitation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatRoomInvitation(PathMetadata metadata) {
        super(ChatRoomInvitation.class, metadata);
    }

}


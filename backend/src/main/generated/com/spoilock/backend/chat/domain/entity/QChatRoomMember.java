package com.spoilock.backend.chat.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatRoomMember is a Querydsl query type for ChatRoomMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomMember extends EntityPathBase<ChatRoomMember> {

    private static final long serialVersionUID = 151543787L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatRoomMember chatRoomMember = new QChatRoomMember("chatRoomMember");

    public final com.spoilock.backend.chat.domain.entity.id.QChatRoomMemberId id;

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final QChatRoom room;

    public final com.spoilock.backend.user.entity.QUser user;

    public QChatRoomMember(String variable) {
        this(ChatRoomMember.class, forVariable(variable), INITS);
    }

    public QChatRoomMember(Path<? extends ChatRoomMember> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatRoomMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatRoomMember(PathMetadata metadata, PathInits inits) {
        this(ChatRoomMember.class, metadata, inits);
    }

    public QChatRoomMember(Class<? extends ChatRoomMember> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.spoilock.backend.chat.domain.entity.id.QChatRoomMemberId(forProperty("id")) : null;
        this.room = inits.isInitialized("room") ? new QChatRoom(forProperty("room"), inits.get("room")) : null;
        this.user = inits.isInitialized("user") ? new com.spoilock.backend.user.entity.QUser(forProperty("user")) : null;
    }

}


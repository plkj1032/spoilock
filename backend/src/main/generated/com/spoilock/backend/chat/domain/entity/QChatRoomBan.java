package com.spoilock.backend.chat.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatRoomBan is a Querydsl query type for ChatRoomBan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomBan extends EntityPathBase<ChatRoomBan> {

    private static final long serialVersionUID = 1074204766L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatRoomBan chatRoomBan = new QChatRoomBan("chatRoomBan");

    public final com.spoilock.backend.common.entity.QCreatedAtEntity _super = new com.spoilock.backend.common.entity.QCreatedAtEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.spoilock.backend.user.entity.QUser kickedBy;

    public final StringPath reason = createString("reason");

    public final DateTimePath<java.time.LocalDateTime> releasedAt = createDateTime("releasedAt", java.time.LocalDateTime.class);

    public final com.spoilock.backend.user.entity.QUser releasedBy;

    public final QChatRoom room;

    public final EnumPath<com.spoilock.backend.chat.domain.type.ChatRoomBanStatus> status = createEnum("status", com.spoilock.backend.chat.domain.type.ChatRoomBanStatus.class);

    public final com.spoilock.backend.user.entity.QUser user;

    public QChatRoomBan(String variable) {
        this(ChatRoomBan.class, forVariable(variable), INITS);
    }

    public QChatRoomBan(Path<? extends ChatRoomBan> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatRoomBan(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatRoomBan(PathMetadata metadata, PathInits inits) {
        this(ChatRoomBan.class, metadata, inits);
    }

    public QChatRoomBan(Class<? extends ChatRoomBan> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.kickedBy = inits.isInitialized("kickedBy") ? new com.spoilock.backend.user.entity.QUser(forProperty("kickedBy")) : null;
        this.releasedBy = inits.isInitialized("releasedBy") ? new com.spoilock.backend.user.entity.QUser(forProperty("releasedBy")) : null;
        this.room = inits.isInitialized("room") ? new QChatRoom(forProperty("room"), inits.get("room")) : null;
        this.user = inits.isInitialized("user") ? new com.spoilock.backend.user.entity.QUser(forProperty("user")) : null;
    }

}


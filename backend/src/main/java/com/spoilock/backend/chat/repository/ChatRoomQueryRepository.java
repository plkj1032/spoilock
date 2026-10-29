package com.spoilock.backend.chat.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.dto.ChatRoomListResponse;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ChatRoomListResponse> findActiveRooms() {
        return queryFactory
                .select(
                        Projections.constructor(
                                ChatRoomListResponse.class,
                                chatRoom.id,
                                new CaseBuilder()
                                        .when(chatRoom.roomType.eq(ChatRoomType.DEFAULT))
                                        .then(chatRoom.drama.title.concat(" 토론방"))
                                        .otherwise(chatRoom.roomName),
                                chatRoom.description,
                                chatRoomMember.id.userId.count(),
                                chatRoom.roomType,
                                chatRoom.createdBy.id,
                                chatRoom.drama.id
                        )
                )
                .from(chatRoom)
                .leftJoin(chatRoomMember)
                .on(chatRoomMember.id.roomId.eq(chatRoom.id))
                .where(
                        chatRoom.active.isTrue(),
                        chatRoom.drama.status.eq(DramaStatus.PUBLISHED),
                        chatRoom.roomType.ne(ChatRoomType.PRIVATE)
                )
                .groupBy(
                        chatRoom.id,
                        chatRoom.roomName,
                        chatRoom.description,
                        chatRoom.roomType,
                        chatRoom.createdBy.id,
                        chatRoom.drama.id,
                        chatRoom.drama.title
                )
                .fetch();
    }

    public Optional<ChatRoomListResponse> findActiveRoomById(Long roomId) {
        ChatRoomListResponse room = queryFactory
                .select(
                        Projections.constructor(
                                ChatRoomListResponse.class,
                                chatRoom.id,
                                new CaseBuilder()
                                        .when(chatRoom.roomType.eq(ChatRoomType.DEFAULT))
                                        .then(chatRoom.drama.title.concat(" 토론방"))
                                        .otherwise(chatRoom.roomName),
                                chatRoom.description,
                                chatRoomMember.id.userId.count(),
                                chatRoom.roomType,
                                chatRoom.createdBy.id,
                                chatRoom.drama.id
                        )
                )
                .from(chatRoom)
                .leftJoin(chatRoomMember)
                .on(chatRoomMember.id.roomId.eq(chatRoom.id))
                .where(
                        chatRoom.id.eq(roomId),
                        chatRoom.active.isTrue(),
                        chatRoom.drama.status.eq(DramaStatus.PUBLISHED)
                )
                .groupBy(
                        chatRoom.id,
                        chatRoom.roomName,
                        chatRoom.description,
                        chatRoom.roomType,
                        chatRoom.createdBy.id,
                        chatRoom.drama.id,
                        chatRoom.drama.title
                )
                .fetchOne();

        return Optional.ofNullable(room);
    }
}
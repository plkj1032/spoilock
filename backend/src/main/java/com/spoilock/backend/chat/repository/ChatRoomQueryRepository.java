package com.spoilock.backend.chat.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.dto.ChatRoomListResponse;
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
                                chatRoom.roomName,
                                chatRoom.description,
                                chatRoomMember.id.userId.count()
                        )
                )
                .from(chatRoom)
                .leftJoin(chatRoomMember)
                .on(chatRoomMember.id.roomId.eq(chatRoom.id))
                .where(chatRoom.active.isTrue())
                .groupBy(
                        chatRoom.id,
                        chatRoom.roomName,
                        chatRoom.description
                )
                .fetch();
    }

    public Optional<ChatRoomListResponse> findActiveRoomById(Long roomId) {
        ChatRoomListResponse room = queryFactory
                .select(
                        Projections.constructor(
                                ChatRoomListResponse.class,
                                chatRoom.id,
                                chatRoom.roomName,
                                chatRoom.description,
                                chatRoomMember.id.userId.count()
                        )
                )
                .from(chatRoom)
                .leftJoin(chatRoomMember)
                .on(chatRoomMember.id.roomId.eq(chatRoom.id))
                .where(
                        chatRoom.id.eq(roomId),
                        chatRoom.active.isTrue()
                )
                .groupBy(
                        chatRoom.id,
                        chatRoom.roomName,
                        chatRoom.description
                )
                .fetchOne();

        return Optional.ofNullable(room);
    }
}
package com.spoilock.backend.chat.repository;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;

@Repository
@RequiredArgsConstructor
public class ChatEmptyRoomQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Transactional(readOnly = true)
    public List<Long> findEmptyRoomIds() {
        return queryFactory
                .select(chatRoom.id)
                .from(chatRoom)
                .where(
                        chatRoom.active.isTrue(),
                        chatRoom.roomType.in(
                                ChatRoomType.CUSTOM,
                                ChatRoomType.PRIVATE
                        ),
                        JPAExpressions
                                .selectOne()
                                .from(chatRoomMember)
                                .where(
                                        chatRoomMember.room.id.eq(chatRoom.id)
                                )
                                .notExists()
                )
                .orderBy(chatRoom.id.asc())
                .limit(100)
                .fetch();
    }

    public Optional<ChatRoom> findLockedById(Long roomId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatRoom)
                        .where(chatRoom.id.eq(roomId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    public boolean hasMembers(Long roomId) {
        return queryFactory
                .selectOne()
                .from(chatRoomMember)
                .where(chatRoomMember.room.id.eq(roomId))
                .fetchFirst() != null;
    }
}
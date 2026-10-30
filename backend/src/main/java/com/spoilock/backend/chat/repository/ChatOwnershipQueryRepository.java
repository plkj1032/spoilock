package com.spoilock.backend.chat.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;

@Repository
@RequiredArgsConstructor
public class ChatOwnershipQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Optional<ChatRoom> findLockedRoom(Long roomId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatRoom)
                        .where(chatRoom.id.eq(roomId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    public boolean hasOtherMembers(Long roomId, Long userId){
        return queryFactory
                .selectOne()
                .from(chatRoomMember)
                .where(
                        chatRoomMember.room.id.eq(roomId),
                        chatRoomMember.user.id.ne(userId)
                )
                .fetchFirst() != null;
    }
}

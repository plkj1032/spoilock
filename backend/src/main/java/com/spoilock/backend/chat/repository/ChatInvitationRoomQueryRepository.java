package com.spoilock.backend.chat.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.dto.ChatInvitationRoomResponse;
import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.drama.enums.DramaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;
import static com.spoilock.backend.chat.domain.entity.QChatRoomBan.chatRoomBan;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;

@Repository
@RequiredArgsConstructor
public class ChatInvitationRoomQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ChatInvitationRoomResponse> findOwnedPrivateRooms(
            Long userId
    ) {
        return queryFactory
                .select(Projections.constructor(
                        ChatInvitationRoomResponse.class,
                        chatRoom.id,
                        chatRoom.roomName,
                        chatRoom.drama.title
                ))
                .from(chatRoom)
                .where(
                        chatRoom.createdBy.id.eq(userId),
                        chatRoom.roomType.eq(ChatRoomType.PRIVATE),
                        chatRoom.active.isTrue(),
                        chatRoom.drama.status.eq(DramaStatus.PUBLISHED),

                        JPAExpressions
                                .selectOne()
                                .from(chatRoomMember)
                                .where(
                                        chatRoomMember.room.id.eq(chatRoom.id),
                                        chatRoomMember.user.id.eq(userId)
                                )
                                .exists(),

                        JPAExpressions
                                .selectOne()
                                .from(chatRoomBan)
                                .where(
                                        chatRoomBan.room.id.eq(chatRoom.id),
                                        chatRoomBan.user.id.eq(userId),
                                        chatRoomBan.status.eq(
                                                ChatRoomBanStatus.ACTIVE
                                        )
                                )
                                .notExists()
                )
                .orderBy(
                        chatRoom.createdAt.desc(),
                        chatRoom.id.desc()
                )
                .fetch();
    }
}
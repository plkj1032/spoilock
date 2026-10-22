package com.spoilock.backend.chat.repository;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;
import static com.spoilock.backend.chat.domain.entity.QChatRoomBan.chatRoomBan;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomMemberRepositoryCustomImpl
                implements ChatRoomMemberRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findUserIdsByRoomId(Long roomId) {

        return queryFactory
                .select(chatRoomMember.id.userId)
                .from(chatRoomMember)
                .where(
                        chatRoomMember.id.roomId.eq(roomId),

                        JPAExpressions
                                .selectOne()
                                .from(chatRoomBan)
                                .where(
                                        chatRoomBan.room.id.eq(
                                                chatRoomMember.id.roomId
                                        ),
                                        chatRoomBan.user.id.eq(
                                                chatRoomMember.id.userId
                                        ),
                                        chatRoomBan.status.eq(
                                                ChatRoomBanStatus.ACTIVE
                                        )
                                )
                                .notExists()
                )
                .fetch();
    }
}

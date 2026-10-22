package com.spoilock.backend.chat.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static com.spoilock.backend.chat.domain.entity.QChatRoomBan.chatRoomBan;


@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomBanRepositoryCustomImpl
        implements ChatRoomBanRepositoryCustom  {
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsActiveBan(Long roomId, Long userId) {
        return queryFactory
                .selectOne()
                .from(chatRoomBan)
                .where(
                        chatRoomBan.room.id.eq(roomId),
                        chatRoomBan.user.id.eq(userId),
                        chatRoomBan.status.eq(ChatRoomBanStatus.ACTIVE)
                )
                .fetchFirst() != null;

    }
}

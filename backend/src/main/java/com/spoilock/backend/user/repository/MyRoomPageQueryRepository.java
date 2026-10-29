package com.spoilock.backend.user.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.common.dto.PageResponse;
import com.spoilock.backend.drama.enums.DramaStatus;
import com.spoilock.backend.user.dto.MyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;
import static com.spoilock.backend.chat.domain.entity.QChatRoomBan.chatRoomBan;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;
import static com.spoilock.backend.drama.entity.QDrama.drama;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyRoomPageQueryRepository {

    private final JPAQueryFactory queryFactory;

    public PageResponse<MyPageResponse.JoinedRoom> findPage(
            Long userId,
            int page,
            int size,
            Long dramaId,
            ChatRoomType roomType
    ) {
        BooleanBuilder condition = new BooleanBuilder()
                .and(chatRoomMember.user.id.eq(userId))
                .and(chatRoom.active.isTrue())
                .and(drama.status.eq(DramaStatus.PUBLISHED))
                .and(
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
                );

        if (dramaId != null) {
            condition.and(drama.id.eq(dramaId));
        }

        if (roomType != null) {
            condition.and(chatRoom.roomType.eq(roomType));
        }

        Long count = queryFactory
                .select(chatRoomMember.user.id.count())
                .from(chatRoomMember)
                .join(chatRoomMember.room, chatRoom)
                .join(chatRoom.drama, drama)
                .where(condition)
                .fetchOne();

        long totalElements = count == null ? 0L : count;

        int lastPage = totalElements == 0
                ? 0
                : (int) ((totalElements - 1) / size);

        int actualPage = Math.min(page, lastPage);

        var content = queryFactory
                .select(Projections.constructor(
                        MyPageResponse.JoinedRoom.class,
                        chatRoom.id,
                        new CaseBuilder()
                                .when(
                                        chatRoom.roomType.eq(
                                                ChatRoomType.DEFAULT
                                        )
                                )
                                .then(drama.title.concat(" 토론방"))
                                .otherwise(chatRoom.roomName),
                        chatRoom.roomType,
                        drama.id,
                        drama.title,
                        chatRoomMember.joinedAt
                ))
                .from(chatRoomMember)
                .join(chatRoomMember.room, chatRoom)
                .join(chatRoom.drama, drama)
                .where(condition)
                .orderBy(
                        chatRoomMember.joinedAt.desc(),
                        chatRoom.id.desc()
                )
                .offset((long) actualPage * size)
                .limit(size)
                .fetch();

        return PageResponse.of(
                content,
                actualPage,
                size,
                totalElements
        );
    }
}
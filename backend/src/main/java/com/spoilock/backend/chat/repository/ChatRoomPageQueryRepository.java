package com.spoilock.backend.chat.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.dto.ChatRoomListResponse;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.common.dto.PageResponse;
import com.spoilock.backend.drama.enums.DramaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomPageQueryRepository {

    private final JPAQueryFactory queryFactory;

    public PageResponse<ChatRoomListResponse> findPage(
            int page,
            int size,
            String keyword,
            Long dramaId,
            ChatRoomType roomType,
            String sort
    ) {
        StringExpression displayName = new CaseBuilder()
                .when(chatRoom.roomType.eq(ChatRoomType.DEFAULT))
                .then(chatRoom.drama.title.concat(" 토론방"))
                .otherwise(chatRoom.roomName);

        BooleanBuilder condition = new BooleanBuilder()
                .and(chatRoom.active.isTrue())
                .and(chatRoom.drama.status.eq(DramaStatus.PUBLISHED));

        if (dramaId != null) {
            condition.and(chatRoom.drama.id.eq(dramaId));
        }

        if (roomType != null) {
            condition.and(chatRoom.roomType.eq(roomType));
        }

        if (keyword != null && !keyword.isBlank()) {
            String search = keyword.trim();

            condition.and(
                    displayName.containsIgnoreCase(search)
                            .or(
                                    chatRoom.drama.title
                                            .containsIgnoreCase(search)
                            )
            );
        }

        Long count = queryFactory
                .select(chatRoom.id.count())
                .from(chatRoom)
                .where(condition)
                .fetchOne();

        long totalElements = count == null ? 0L : count;

        int lastPage = totalElements == 0
                ? 0
                : (int) ((totalElements - 1) / size);

        int actualPage = Math.min(page, lastPage);

        OrderSpecifier<?>[] orderBy;

        if ("name".equals(sort)) {
            orderBy = new OrderSpecifier<?>[]{
                    displayName.asc(),
                    chatRoom.id.asc()
            };
        } else {
            orderBy = new OrderSpecifier<?>[]{
                    chatRoomMember.id.userId.count().desc(),
                    displayName.asc(),
                    chatRoom.id.asc()
            };
        }

        var content = queryFactory
                .select(Projections.constructor(
                        ChatRoomListResponse.class,
                        chatRoom.id,
                        displayName,
                        chatRoom.description,
                        chatRoomMember.id.userId.count(),
                        chatRoom.roomType,
                        chatRoom.createdBy.id,
                        chatRoom.drama.id
                ))
                .from(chatRoom)
                .leftJoin(chatRoomMember)
                .on(chatRoomMember.id.roomId.eq(chatRoom.id))
                .where(condition)
                .groupBy(
                        chatRoom.id,
                        chatRoom.roomName,
                        chatRoom.description,
                        chatRoom.roomType,
                        chatRoom.createdBy.id,
                        chatRoom.drama.id,
                        chatRoom.drama.title
                )
                .orderBy(orderBy)
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
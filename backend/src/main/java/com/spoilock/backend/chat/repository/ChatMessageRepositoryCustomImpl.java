package com.spoilock.backend.chat.repository;


import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spoilock.backend.chat.domain.entity.QChatMessage.chatMessage;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessage> findHistory(
            Long roomId,
            Long cursor,
            int limit
    ) {
        return queryFactory
                .selectFrom(chatMessage)

                // 이후 응답 생성 시 추가 조회가 발생하지 않도록 함께 조회
                .join(chatMessage.room).fetchJoin()
                .join(chatMessage.user).fetchJoin()

                .where(
                        chatMessage.room.id.eq(roomId),
                        cursorLt(cursor)
                )

                // 최신 메시지부터 조회
                .orderBy(chatMessage.id.desc())

                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorLt(Long cursor) {
        if (cursor == null) {
            return null;
        }

        return chatMessage.id.lt(cursor);
    }

}

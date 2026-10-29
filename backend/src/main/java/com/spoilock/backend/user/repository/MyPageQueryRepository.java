package com.spoilock.backend.user.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.user.dto.MyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;
import static com.spoilock.backend.chat.domain.entity.QChatRoomBan.chatRoomBan;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;
import static com.spoilock.backend.drama.domain.entity.QDrama.drama;
import static com.spoilock.backend.drama.domain.entity.QUserProgress.userProgress;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MyPageResponse.WatchingDrama> findWatchingDramas(Long userId){
        return queryFactory
                .select(Projections.constructor(
                        MyPageResponse.WatchingDrama.class,
                        drama.id,
                        drama.title,
                        drama.posterUrl,
                        drama.totalEpisodeCount,
                        userProgress.watchedEpisodeNumber,
                        userProgress.updatedAt
                ))
                .from(userProgress)
                .join(userProgress.drama,drama)
                .where(
                        userProgress.user.id.eq(userId),
                        drama.status.eq(DramaStatus.PUBLISHED))
                .orderBy(
                        userProgress.updatedAt.desc(),
                        drama.id.desc())
                .fetch();
    }

    public List<MyPageResponse.JoinedRoom> findJoinedRooms(Long userId){
        return queryFactory
                .select(Projections.constructor(
                        MyPageResponse.JoinedRoom.class,
                        chatRoom.id,
                        new CaseBuilder()
                                .when(
                                        chatRoom.roomType.eq(ChatRoomType.DEFAULT))
                                .then(drama.title.concat("토론방"))
                                .otherwise(chatRoom.roomName),
                        chatRoom.roomType,
                        drama.id,
                        drama.title,
                        chatRoomMember.joinedAt))
                .from(chatRoomMember)
                .join(chatRoomMember.room, chatRoom)
                .join(chatRoom.drama,drama)
                .where(
                        chatRoomMember.user.id.eq(userId),
                        chatRoom.active.isTrue(),
                        drama.status.eq(DramaStatus.PUBLISHED),
                        JPAExpressions
                                .selectOne()
                                .from(chatRoomBan)
                                .where(
                                        chatRoomBan.room.id.eq(chatRoom.id),
                                        chatRoomBan.user.id.eq(userId),
                                        chatRoomBan.status.eq(
                                                ChatRoomBanStatus.ACTIVE))
                                .notExists())
                .orderBy(
                        chatRoomMember.joinedAt.desc(),
                        chatRoom.id.desc())
                .fetch();
    }
}

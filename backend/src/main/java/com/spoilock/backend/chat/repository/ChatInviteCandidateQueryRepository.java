package com.spoilock.backend.chat.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.type.ChatInvitationStatus;
import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import com.spoilock.backend.user.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.spoilock.backend.follow.entity.QUserFollow.userFollow;
import static com.spoilock.backend.chat.domain.entity.QChatRoomMember.chatRoomMember;
import static com.spoilock.backend.chat.domain.entity.QChatRoomBan.chatRoomBan;
import static com.spoilock.backend.chat.domain.entity.QChatRoomInvitation.chatRoomInvitation;

@Repository
@RequiredArgsConstructor
public class ChatInviteCandidateQueryRepository {

    private final JPAQueryFactory queryFactory;

    public record Candidate(
            Long userId,
            String nickname,
            String status
    ) {
    }

    public record CandidatePage(
            List<Candidate> items,
            boolean hasNext
    ) {
    }

    public CandidatePage findPage(
            Long roomId,
            Long senderId,
            int page
    ) {
        int size = 5;
        LocalDateTime now = LocalDateTime.now();

        var joined = JPAExpressions
                .selectOne()
                .from(chatRoomMember)
                .where(
                        chatRoomMember.room.id.eq(roomId),
                        chatRoomMember.user.id.eq(userFollow.following.id)
                )
                .exists();

        var banned = JPAExpressions
                .selectOne()
                .from(chatRoomBan)
                .where(
                        chatRoomBan.room.id.eq(roomId),
                        chatRoomBan.user.id.eq(userFollow.following.id),
                        chatRoomBan.status.eq(ChatRoomBanStatus.ACTIVE)
                )
                .exists();

        var invited = JPAExpressions
                .selectOne()
                .from(chatRoomInvitation)
                .where(
                        chatRoomInvitation.roomId.eq(roomId),
                        chatRoomInvitation.receiverUserId.eq(
                                userFollow.following.id
                        ),
                        chatRoomInvitation.status.eq(
                                ChatInvitationStatus.PENDING
                        ),
                        chatRoomInvitation.expiresAt.gt(now)
                )
                .exists();

        var status = new CaseBuilder()
                .when(
                        userFollow.following.accountStatus
                                .ne(AccountStatus.ACTIVE)
                )
                .then("UNAVAILABLE")
                .when(banned)
                .then("BANNED")
                .when(joined)
                .then("JOINED")
                .when(invited)
                .then("INVITED")
                .otherwise("AVAILABLE");

        var rows = queryFactory
                .select(Projections.constructor(
                        Candidate.class,
                        userFollow.following.id,
                        userFollow.following.nickname,
                        status
                ))
                .from(userFollow)
                .where(userFollow.follower.id.eq(senderId))
                .orderBy(
                        userFollow.createdAt.desc(),
                        userFollow.following.id.desc()
                )
                .offset((long) page * size)
                .limit(size + 1)
                .fetch();

        boolean hasNext = rows.size() > size;

        return new CandidatePage(
                List.copyOf(rows.subList(0, Math.min(size, rows.size()))),
                hasNext
        );
    }
}
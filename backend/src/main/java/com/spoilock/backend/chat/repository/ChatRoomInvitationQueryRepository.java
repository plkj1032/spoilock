package com.spoilock.backend.chat.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.ChatRoomInvitation;
import com.spoilock.backend.chat.domain.type.ChatInvitationStatus;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.spoilock.backend.chat.domain.entity.QChatRoomInvitation.chatRoomInvitation;

@Repository
@RequiredArgsConstructor
public class ChatRoomInvitationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public boolean existsPendingInvitation(
            Long roomId,
            Long receiverUserId,
            LocalDateTime now
    ) {
        return queryFactory
                .selectOne()
                .from(chatRoomInvitation)
                .where(
                        chatRoomInvitation.roomId.eq(roomId),
                        chatRoomInvitation.receiverUserId.eq(receiverUserId),
                        chatRoomInvitation.status.eq(
                                ChatInvitationStatus.PENDING
                        ),
                        chatRoomInvitation.expiresAt.gt(now)
                )
                .fetchFirst() != null;
    }

    public Optional<Long> findRoomIdByInvitationId(Long invitationId) {
        return Optional.ofNullable(
                queryFactory
                        .select(chatRoomInvitation.roomId)
                        .from(chatRoomInvitation)
                        .where(chatRoomInvitation.id.eq(invitationId))
                        .fetchOne()
        );
    }

    public Optional<Long> findInvitationIdByCodeHash(String codeHash) {
        return Optional.ofNullable(
                queryFactory
                        .select(chatRoomInvitation.id)
                        .from(chatRoomInvitation)
                        .where(chatRoomInvitation.codeHash.eq(codeHash))
                        .fetchOne()
        );
    }

    public Optional<ChatRoomInvitation> findLockedById(Long invitationId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatRoomInvitation)
                        .where(chatRoomInvitation.id.eq(invitationId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    public List<ChatRoomInvitation> findForReceiver(
            Long receiverId,
            List<Long> invitationIds
    ) {
        if (invitationIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(chatRoomInvitation)
                .where(
                        chatRoomInvitation.receiverUserId.eq(receiverId),
                        chatRoomInvitation.id.in(invitationIds)
                )
                .fetch();
    }
}
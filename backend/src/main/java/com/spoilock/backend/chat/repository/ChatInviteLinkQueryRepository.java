package com.spoilock.backend.chat.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.ChatInviteLink;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.spoilock.backend.chat.domain.entity.QChatInviteLink.chatInviteLink;
import static com.spoilock.backend.chat.domain.entity.QChatInviteLinkUse.chatInviteLinkUse;
import static com.spoilock.backend.chat.domain.entity.QChatRoom.chatRoom;

@Repository
@RequiredArgsConstructor
public class ChatInviteLinkQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 발급·취소·수락을 같은 방 단위로 순서대로 처리
    public Optional<ChatRoom> findLockedRoom(Long roomId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatRoom)
                        .where(chatRoom.id.eq(roomId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    // 해당 방에서 가장 최근에 발급한 공유 링크
    public Optional<ChatInviteLink> findLatest(Long roomId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatInviteLink)
                        .where(chatInviteLink.roomId.eq(roomId))
                        .orderBy(chatInviteLink.id.desc())
                        .fetchFirst()
        );
    }

    // 방 → 링크 순서로 잠그기 위해 방 ID부터 조회
    public Optional<Long> findRoomIdByCodeHash(String codeHash) {
        return Optional.ofNullable(
                queryFactory
                        .select(chatInviteLink.roomId)
                        .from(chatInviteLink)
                        .where(chatInviteLink.codeHash.eq(codeHash))
                        .fetchOne()
        );
    }

    public Optional<ChatInviteLink> findLockedByCodeHash(
            String codeHash
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(chatInviteLink)
                        .where(chatInviteLink.codeHash.eq(codeHash))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    public boolean existsUsage(
            Long linkId,
            Long userId
    ) {
        return queryFactory
                .selectOne()
                .from(chatInviteLinkUse)
                .where(
                        chatInviteLinkUse.linkId.eq(linkId),
                        chatInviteLinkUse.userId.eq(userId)
                )
                .fetchFirst() != null;
    }
}
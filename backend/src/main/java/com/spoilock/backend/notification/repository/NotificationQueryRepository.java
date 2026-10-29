package com.spoilock.backend.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.notification.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.spoilock.backend.notification.domain.entity.QNotification.notification;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Notification> findLatest(
            Long receiverId,
            int limit
    ) {
        return queryFactory
                .selectFrom(notification)
                .where(
                        notification.receiverId.eq(receiverId),
                        notification.dismissedAt.isNull()
                )
                .orderBy(
                        notification.createdAt.desc(),
                        notification.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public long countUnread(Long receiverId) {
        Long count = queryFactory
                .select(notification.id.count())
                .from(notification)
                .where(
                        notification.receiverId.eq(receiverId),
                        notification.dismissedAt.isNull(),
                        notification.readAt.isNull()
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    public long markAsRead(
            Long notificationId,
            Long receiverId
    ) {
        return queryFactory
                .update(notification)
                .set(notification.readAt, LocalDateTime.now())
                .where(
                        notification.id.eq(notificationId),
                        notification.receiverId.eq(receiverId),
                        notification.dismissedAt.isNull(),
                        notification.readAt.isNull()
                )
                .execute();
    }

    public long dismiss(
            Long notificationId,
            Long receiverId
    ) {
        return queryFactory
                .update(notification)
                .set(notification.dismissedAt, LocalDateTime.now())
                .where(
                        notification.id.eq(notificationId),
                        notification.receiverId.eq(receiverId),
                        notification.dismissedAt.isNull()
                )
                .execute();
    }

    public long dismissAll(Long receiverId){
        return queryFactory
                .update(notification)
                .set(notification.dismissedAt, LocalDateTime.now())
                .where(
                        notification.receiverId.eq(receiverId),
                        notification.dismissedAt.isNull())
                .execute();
    }

    public boolean existsForReceiver(
            Long notificationId,
            Long receiverId
    ) {
        return queryFactory
                .selectOne()
                .from(notification)
                .where(
                        notification.id.eq(notificationId),
                        notification.receiverId.eq(receiverId)
                )
                .fetchFirst() != null;
    }
    public void markInvitationAsRead(
            Long invitationId,
            Long receiverId
    ) {
        queryFactory
                .update(notification)
                .set(notification.readAt, LocalDateTime.now())
                .where(
                        notification.invitationId.eq(invitationId),
                        notification.receiverId.eq(receiverId),
                        notification.readAt.isNull()
                )
                .execute();
    }
}
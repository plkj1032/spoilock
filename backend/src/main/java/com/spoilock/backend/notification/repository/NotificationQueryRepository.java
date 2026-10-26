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

    public List<Notification> findPage(
            Long receiveId,
            Long cursor,
            int limit

    ){
        return queryFactory
                .selectFrom(notification)
                .where(
                        notification.receiverId.eq(receiveId),
                        cursor == null ? null : notification.id.lt(cursor))
                .orderBy(notification.id.desc())
                .limit(limit)
                .fetch();
    }

    public long countUnread(Long receiveId){
        Long count = queryFactory
                .select(notification.id.count())
                .from(notification)
                .where(
                        notification.receiverId.eq(receiveId),
                        notification.readAt.isNull())
                .fetchOne();

        return count == null ? 0L : count;
    }

    public long markAsRead(
            Long notificationId,
            Long receiverId
    ){
        return queryFactory
                .update(notification)
                .set(notification.readAt, LocalDateTime.now())
                .where(
                        notification.id.eq(notificationId),
                        notification.receiverId.eq(receiverId),
                        notification.readAt.isNull())
                .execute();
    }

    public boolean existsForReceiver(
            Long notificationId,
            Long receiverId
    ){
        return queryFactory
                .selectOne()
                .from(notification)
                .where(
                        notification.id.eq(notificationId),
                        notification.receiverId.eq(receiverId))
                .fetchFirst() != null;
    }
}

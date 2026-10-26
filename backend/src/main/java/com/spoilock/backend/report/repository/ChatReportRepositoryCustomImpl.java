package com.spoilock.backend.report.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.report.entity.ChatReport;
import com.spoilock.backend.report.enums.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spoilock.backend.report.entity.QChatReport.chatReport;

@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ChatReportRepositoryCustomImpl implements ChatReportRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByMessageAndReporter(Long messageId, Long reporterId) {
        return queryFactory
                .selectOne()
                .from(chatReport)
                .where(
                        chatReport.message.id.eq(messageId),
                        chatReport.reporter.id.eq(reporterId))
                .fetchFirst() != null;
    }

    @Override
    public long countPendingByRoomAndTarget(Long roomId, Long targetUserId) {
        Long count = queryFactory
                .select(chatReport.id.count())
                .from(chatReport)
                .where(
                        chatReport.message.room.id.eq(roomId),
                        chatReport.message.user.id.eq(targetUserId),
                        chatReport.status.eq(ReportStatus.PENDING))
                .fetchOne();

        return count == null ? 0L : count;
    }

    @Override
    public List<ChatReport> findPendingByRoomAndTarget(Long roomId, Long targetUserId) {
        return queryFactory
                .selectFrom(chatReport)
                .where(
                        chatReport.message.room.id.eq(roomId),
                        chatReport.message.user.id.eq(targetUserId),
                        chatReport.status.eq(ReportStatus.PENDING))
                .orderBy(
                        chatReport.createdAt.asc(),
                        chatReport.id.asc())
                .fetch();
    }
}

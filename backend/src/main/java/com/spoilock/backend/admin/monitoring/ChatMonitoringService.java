package com.spoilock.backend.admin.monitoring;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.QChatMessage;
import com.spoilock.backend.chat.domain.entity.QChatRoom;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.drama.domain.entity.QDrama;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ChatMonitoringService {

    private final JPAQueryFactory queryFactory;

    private static NumberExpression<Long> countIf(BooleanExpression expression) {
        return new CaseBuilder().when(expression).then(1L).otherwise(0L).sum();
    }

    private static long n(Long value) {
        return value == null ? 0 : value;
    }

    @Transactional(readOnly = true)
    public ChatMonitoringResponse getDashboard(String range, Long dramaId) {
        if (!"24h".equals(range) && !"7d".equals(range)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range must be 24h or 7d");
        }
        if (dramaId != null && dramaId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid dramaId");
        }

        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours("7d".equals(range) ? 168 : 24);
        QChatMessage cm = QChatMessage.chatMessage;
        QChatRoom room = QChatRoom.chatRoom;
        QDrama drama = QDrama.drama;

        BooleanExpression condition = cm.messageType.eq(MessageType.CHAT)
                .and(cm.createdAt.goe(from))
                .and(cm.createdAt.lt(to));
        if (dramaId != null) {
            condition = condition.and(room.drama.id.eq(dramaId));
        }

        NumberExpression<Long> totalCount = cm.count();
        NumberExpression<Long> pendingCount = countIf(cm.analysisStatus.eq(AnalysisStatus.PENDING));
        NumberExpression<Long> processingCount = countIf(cm.analysisStatus.eq(AnalysisStatus.PROCESSING));
        NumberExpression<Long> completedCount = countIf(cm.analysisStatus.eq(AnalysisStatus.COMPLETED));
        NumberExpression<Long> failedCount = countIf(cm.analysisStatus.eq(AnalysisStatus.FAILED));
        NumberExpression<Long> publicCount = countIf(cm.analysisOutcome.eq("PUBLIC"));
        NumberExpression<Long> gatedCount = countIf(cm.analysisOutcome.eq("EPISODE_GATED"));
        NumberExpression<Long> heldCount = countIf(cm.analysisOutcome.eq("HELD"));

        Tuple base = queryFactory.select(totalCount, pendingCount, processingCount,
                        completedCount, failedCount, publicCount, gatedCount, heldCount)
                .from(cm)
                .join(cm.room, room)
                .where(condition)
                .fetchOne();

        BooleanExpression measured = cm.analysisStatus.in(AnalysisStatus.COMPLETED, AnalysisStatus.FAILED)
                .and(cm.analysisDurationMs.isNotNull());
        Double avg = queryFactory.select(cm.analysisDurationMs.avg())
                .from(cm).join(cm.room, room)
                .where(condition, measured)
                .fetchOne();
        Long measuredCount = queryFactory.select(cm.count())
                .from(cm).join(cm.room, room)
                .where(condition, measured)
                .fetchOne();

        Long p95 = null;
        if (n(measuredCount) > 0) {
            long offset = Math.max(0, (long) Math.ceil(measuredCount * 0.95) - 1);
            p95 = queryFactory.select(cm.analysisDurationMs)
                    .from(cm).join(cm.room, room)
                    .where(condition, measured)
                    .orderBy(cm.analysisDurationMs.asc(), cm.id.asc())
                    .offset(offset).limit(1)
                    .fetchOne();
        }

        ChatMonitoringResponse.Summary summary = new ChatMonitoringResponse.Summary(
                base == null ? 0 : n(base.get(totalCount)),
                base == null ? 0 : n(base.get(pendingCount)),
                base == null ? 0 : n(base.get(processingCount)),
                base == null ? 0 : n(base.get(completedCount)),
                base == null ? 0 : n(base.get(failedCount)),
                base == null ? 0 : n(base.get(publicCount)),
                base == null ? 0 : n(base.get(gatedCount)),
                base == null ? 0 : n(base.get(heldCount)),
                avg == null ? null : Math.round(avg),
                p95
        );

        // DB에서 최대 168개의 시간대만 집계. 7d일 때는 Java에서 일별로 합친다.
        NumberExpression<Integer> year = cm.createdAt.year();
        NumberExpression<Integer> month = cm.createdAt.month();
        NumberExpression<Integer> day = cm.createdAt.dayOfMonth();
        NumberExpression<Integer> hour = cm.createdAt.hour();
        NumberExpression<Long> groupedCount = cm.count();
        List<Tuple> grouped = queryFactory
                .select(year, month, day, hour, groupedCount, gatedCount, failedCount)
                .from(cm).join(cm.room, room)
                .where(condition)
                .groupBy(year, month, day, hour)
                .fetch();

        Map<String, long[]> buckets = new TreeMap<>();
        for (Tuple row : grouped) {
            String ymd = String.format("%04d-%02d-%02d",
                    row.get(year), row.get(month), row.get(day));
            String key = "7d".equals(range)
                    ? ymd
                    : String.format("%s %02d:00", ymd, row.get(hour));
            long[] values = buckets.computeIfAbsent(key, ignored -> new long[3]);
            values[0] += n(row.get(groupedCount));
            values[1] += n(row.get(gatedCount));
            values[2] += n(row.get(failedCount));
        }
        List<ChatMonitoringResponse.Bucket> timeline = new ArrayList<>();
        buckets.forEach((period, values) -> timeline.add(new ChatMonitoringResponse.Bucket(
                period, values[0], values[1], values[2]
        )));

        List<ChatMonitoringResponse.Recent> recent = queryFactory
                .select(
                        cm.id,
                        room.drama.id,
                        drama.title,
                        cm.content,               // 채팅 원문
                        cm.analysisStatus,
                        cm.analysisOutcome,
                        cm.analysisPolicyCode,
                        cm.analysisDurationMs,
                        cm.createdAt,
                        cm.analyzedAt
                )
                .from(cm)
                .join(cm.room, room)
                .join(room.drama, drama)
                .where(condition)
                .orderBy(cm.createdAt.desc(), cm.id.desc())
                .limit(20)
                .fetch()
                .stream()
                .map(row -> new ChatMonitoringResponse.Recent(
                        row.get(cm.id),
                        row.get(room.drama.id),
                        row.get(drama.title),
                        row.get(cm.content),
                        row.get(cm.analysisStatus) == null
                                ? null
                                : row.get(cm.analysisStatus).name(),
                        row.get(cm.analysisOutcome),
                        row.get(cm.analysisPolicyCode),
                        row.get(cm.analysisDurationMs),
                        row.get(cm.createdAt),
                        row.get(cm.analyzedAt)
                ))
                .toList();

        List<ChatMonitoringResponse.DramaOption> dramas = queryFactory
                .select(drama.id, drama.title)
                .from(drama)
                .orderBy(drama.title.asc(), drama.id.asc())
                .fetch().stream()
                .map(row -> new ChatMonitoringResponse.DramaOption(row.get(drama.id), row.get(drama.title)))
                .toList();

        return new ChatMonitoringResponse(range, from, to, summary, timeline, recent, dramas);
    }
}

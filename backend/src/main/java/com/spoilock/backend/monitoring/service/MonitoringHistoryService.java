
package com.spoilock.backend.monitoring.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.QChatMessage;
import com.spoilock.backend.chat.domain.entity.QChatRoom;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.drama.domain.entity.QDrama;
import com.spoilock.backend.drama.domain.entity.QEpisode;
import com.spoilock.backend.monitoring.domain.dto.MonitoringPage;
import com.spoilock.backend.spoiler.entity.QEpisodeAnalysis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitoringHistoryService {

    private final JPAQueryFactory queryFactory;
    private static final int PAGE_SIZE = 20;


    public record ChatRow(
            Long messageId,
            Long dramaId,
            String dramaTitle,
            String content,
            String analysisStatus,
            String analysisOutcome,

            // FastAPI 원본 판정
            String rawVerdict,

            // Spring 최종 정책
            Integer requiredEpisodeNumber,
            Boolean requiredUntilFinale,

            String policyCode,
            String policyDetail,
            Long durationMs,
            LocalDateTime createdAt,
            LocalDateTime analyzedAt
    ) {}


    public record JobRow(
            Long episodeId,
            String dramaTitle,
            String source,
            String status,
            Integer progress,
            Integer eventCount,
            LocalDateTime requestedAt
    ) {}

    private void validate(int page, Long dramaId) {
        if (page < 0 || page > 100000
                || (dramaId != null && dramaId <= 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid paging parameters"
            );
        }
    }

    /**
     * 전체 채팅 판정 이력
     * 통계의 24h/7d 필터와 별개로 과거 전체 기록 조회
     */
    @Transactional(readOnly = true)
    public MonitoringPage<ChatRow> chatHistory(
            int page, Long dramaId
    ) {
        validate(page, dramaId);

        QChatMessage cm = QChatMessage.chatMessage;
        QChatRoom room = QChatRoom.chatRoom;
        QDrama drama = QDrama.drama;

        BooleanExpression condition =
                cm.messageType.eq(MessageType.CHAT);

        if (dramaId != null) {
            condition = condition.and(drama.id.eq(dramaId));
        }

        Long count = queryFactory.select(cm.count())
                .from(cm)
                .join(cm.room, room)
                .join(room.drama, drama)
                .where(condition)
                .fetchOne();

        List<ChatRow> rows = queryFactory
                .select(
                        cm.id,
                        drama.id,
                        drama.title,
                        cm.content,
                        cm.analysisStatus,
                        cm.analysisOutcome,

                        cm.analysisRawVerdict,
                        cm.requiredEpisodeNumber,
                        cm.requiredUntilFinale,

                        cm.analysisPolicyCode,
                        cm.analysisPolicyDetail,
                        cm.analysisDurationMs,
                        cm.createdAt,
                        cm.analyzedAt
                )
                .from(cm)
                .join(cm.room, room)
                .join(room.drama, drama)
                .where(condition)
                .orderBy(cm.createdAt.desc(), cm.id.desc())
                .offset((long) page * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .fetch()
                .stream()

                .map(row -> new ChatRow(
                        row.get(cm.id),
                        row.get(drama.id),
                        row.get(drama.title),
                        row.get(cm.content),

                        row.get(cm.analysisStatus) == null
                                ? null
                                : row.get(cm.analysisStatus).name(),

                        row.get(cm.analysisOutcome),

                        row.get(cm.analysisRawVerdict),
                        row.get(cm.requiredEpisodeNumber),
                        row.get(cm.requiredUntilFinale),

                        row.get(cm.analysisPolicyCode),
                        row.get(cm.analysisPolicyDetail),
                        row.get(cm.analysisDurationMs),
                        row.get(cm.createdAt),
                        row.get(cm.analyzedAt)
                ))

                .toList();

        return MonitoringPage.of(
                rows, count == null ? 0 : count, page, PAGE_SIZE
        );
    }

    /**
     * 자막 / 영상 AI 분석 회차 이력
     */
    @Transactional(readOnly = true)
    public MonitoringPage<JobRow> jobHistory(
            int page, Long dramaId
    ) {
        validate(page, dramaId);

        QEpisodeAnalysis a = QEpisodeAnalysis.episodeAnalysis;
        QEpisode ep = QEpisode.episode;
        QDrama drama = QDrama.drama;

        BooleanExpression condition =
                a.analysisRequestedAt.isNotNull();

        if (dramaId != null) {
            condition = condition.and(drama.id.eq(dramaId));
        }

        Long count = queryFactory.select(a.count())
                .from(a)
                .join(a.episode, ep)
                .join(ep.drama, drama)
                .where(condition)
                .fetchOne();

        List<JobRow> rows = queryFactory
                .select(
                        ep.id,
                        drama.title,
                        a.analysisSource,
                        a.analysisStatus,
                        a.analysisProgress,
                        a.eventCount,
                        a.analysisRequestedAt
                )
                .from(a)
                .join(a.episode, ep)
                .join(ep.drama, drama)
                .where(condition)
                .orderBy(a.analysisRequestedAt.desc(), a.id.desc())
                .offset((long) page * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .fetch()
                .stream()
                .map(row -> new JobRow(
                        row.get(ep.id),
                        row.get(drama.title),
                        row.get(a.analysisSource) == null
                                ? null : row.get(a.analysisSource).name(),
                        row.get(a.analysisStatus) == null
                                ? null : row.get(a.analysisStatus).name(),
                        row.get(a.analysisProgress),
                        row.get(a.eventCount),
                        row.get(a.analysisRequestedAt)
                ))
                .toList();

        return MonitoringPage.of(
                rows, count == null ? 0 : count, page, PAGE_SIZE
        );
    }
}

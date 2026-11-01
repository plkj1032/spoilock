package com.spoilock.backend.admin.monitoring;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.drama.domain.entity.QDrama;
import com.spoilock.backend.drama.domain.entity.QEpisode;
import com.spoilock.backend.spoiler.entity.QEpisodeAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvancedMonitoringService {
    private final JPAQueryFactory queryFactory;
    private final RedisConnectionFactory redisFactory;
    private final SpoilerApiProperties spoilerProperties;

    public record ServiceStatus(String name, String status, String detail) {}
    public record JobSummary(long total, long pending, long processing, long completed, long failed) {}
    public record JobRow(Long episodeId, String dramaTitle, String source,
                         String status, Integer progress, Integer eventCount, LocalDateTime requestedAt) {}
    public record TraceRow(String stage, Long durationMs, LocalDateTime recordedAt) {}
    public record AdvancedResponse(String nodeName, Long heapUsedMb, Long heapMaxMb,
                                   List<ServiceStatus> services, JobSummary jobs,
                                   List<JobRow> recentJobs, List<TraceRow> trace) {}

    private static NumberExpression<Long> countIf(BooleanExpression condition) {
        return new CaseBuilder().when(condition).then(1L).otherwise(0L).sum();
    }
    private static long n(Long value) { return value == null ? 0 : value; }

    public AdvancedResponse getDashboard(String range, Long dramaId, Long messageId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = "7d".equals(range) ? now.minusDays(7) : now.minusHours(24);

        QEpisodeAnalysis a = QEpisodeAnalysis.episodeAnalysis;
        QEpisode episode = QEpisode.episode;
        QDrama drama = QDrama.drama;
        BooleanExpression where = a.analysisRequestedAt.goe(from)
                .and(a.analysisRequestedAt.lt(now));
        if (dramaId != null) where = where.and(drama.id.eq(dramaId));

        NumberExpression<Long> pending = countIf(a.analysisStatus.eq(AnalysisStatus.PENDING));
        NumberExpression<Long> processing = countIf(a.analysisStatus.eq(AnalysisStatus.PROCESSING));
        NumberExpression<Long> completed = countIf(a.analysisStatus.eq(AnalysisStatus.COMPLETED));
        NumberExpression<Long> failed = countIf(a.analysisStatus.eq(AnalysisStatus.FAILED));
        NumberExpression<Long> total = a.count();

        Tuple jobCounts = queryFactory
                .select(total, pending, processing, completed, failed)
                .from(a).join(a.episode, episode).join(episode.drama, drama)
                .where(where)
                .fetchOne();
        JobSummary summary = jobCounts == null
                ? new JobSummary(0, 0, 0, 0, 0)
                : new JobSummary(n(jobCounts.get(total)), n(jobCounts.get(pending)),
                        n(jobCounts.get(processing)), n(jobCounts.get(completed)), n(jobCounts.get(failed)));

        List<JobRow> jobs = queryFactory
                .select(episode.id, drama.title, a.analysisSource, a.analysisStatus,
                        a.analysisProgress, a.eventCount, a.analysisRequestedAt)
                .from(a).join(a.episode, episode).join(episode.drama, drama)
                .where(where)
                .orderBy(a.analysisRequestedAt.desc(), a.id.desc())
                .limit(20)
                .fetch().stream()
                .map(row -> new JobRow(
                        row.get(episode.id), row.get(drama.title),
                        row.get(a.analysisSource) == null ? null : row.get(a.analysisSource).name(),
                        row.get(a.analysisStatus) == null ? null : row.get(a.analysisStatus).name(),
                        row.get(a.analysisProgress), row.get(a.eventCount), row.get(a.analysisRequestedAt)))
                .toList();

        List<TraceRow> trace = List.of();
        if (messageId != null) {
            QChatAnalysisTrace t = QChatAnalysisTrace.chatAnalysisTrace;
            trace = queryFactory.select(t.stage, t.durationMs, t.recordedAt)
                    .from(t)
                    .where(t.messageId.eq(messageId))
                    .orderBy(t.recordedAt.asc(), t.id.asc())
                    .limit(60)
                    .fetch().stream()
                    .map(row -> new TraceRow(row.get(t.stage), row.get(t.durationMs), row.get(t.recordedAt)))
                    .toList();
        }

        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long mb = 1024L * 1024;
        List<ServiceStatus> services = new ArrayList<>();
        services.add(new ServiceStatus("Spring Boot", "OK", "관리자 API 응답 중"));
        services.add(new ServiceStatus("MySQL", "OK", "통계 조회 성공"));
        services.add(checkRedis());
        services.add(checkFastApiTcp());

        return new AdvancedResponse(
                nodeName(), heap.getUsed() / mb,
                heap.getMax() < 0 ? null : heap.getMax() / mb,
                services, summary, jobs, trace);
    }

    private ServiceStatus checkRedis() {
        try (var connection = redisFactory.getConnection()) {
            boolean ok = "PONG".equalsIgnoreCase(connection.ping());
            return new ServiceStatus("Redis", ok ? "OK" : "DOWN",
                    ok ? "PING 응답 정상" : "PING 실패");
        } catch (Exception ex) {
            return new ServiceStatus("Redis", "DOWN",
                    "연결 실패: " + ex.getClass().getSimpleName());
        }
    }

    private ServiceStatus checkFastApiTcp() {
        try {
            URI uri = URI.create(spoilerProperties.baseUrl());
            String host = uri.getHost();
            if (host == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("invalid base URL");
            }
            int port = uri.getPort() >= 0 ? uri.getPort()
                    : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 1500);
            }
            return new ServiceStatus("FastAPI TCP", "OK", "포트 연결 성공 (AI /judge 검증 아님)");
        } catch (Exception ex) {
            return new ServiceStatus("FastAPI TCP", "DOWN",
                    "포트 연결 실패: " + ex.getClass().getSimpleName());
        }
    }

    private static String nodeName() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "unknown-node"; }
    }
}

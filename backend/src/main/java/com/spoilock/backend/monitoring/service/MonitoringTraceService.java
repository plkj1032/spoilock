package com.spoilock.backend.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringTraceService {
    private final MonitoringTraceWriter writer;
    private final MonitoringRealtimeNotifier realtimeNotifier;

    public void record(Long messageId, String stage, Long durationMs) {
        try {
            writer.write(messageId, stage, durationMs);
        } catch (RuntimeException ex) {
            // 모니터링 기록 실패 때문에 채팅 기능이 중단되면 안 된다.
            log.warn("[MONITOR_TRACE_FAILED] messageId={} stage={} errorType={}",
                    messageId, stage, ex.getClass().getSimpleName());
        }
        //기록 저장 실패 시에도 최신 분석 상태를 다시 조회하도록 알린다
        // 알림 발행 실패는 notifier 내부에서 처리
        realtimeNotifier.publish(messageId,stage);
    }
}

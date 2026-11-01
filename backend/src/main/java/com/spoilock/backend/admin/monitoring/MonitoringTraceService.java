package com.spoilock.backend.admin.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringTraceService {
    private final MonitoringTraceWriter writer;

    public void record(Long messageId, String stage, Long durationMs) {
        try {
            writer.write(messageId, stage, durationMs);
        } catch (RuntimeException ex) {
            // 모니터링 기록 실패 때문에 채팅 기능이 중단되면 안 된다.
            log.warn("[MONITOR_TRACE_FAILED] messageId={} stage={} errorType={}",
                    messageId, stage, ex.getClass().getSimpleName());
        }
    }
}

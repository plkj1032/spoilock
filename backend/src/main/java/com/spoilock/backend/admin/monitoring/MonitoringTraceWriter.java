package com.spoilock.backend.admin.monitoring;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonitoringTraceWriter {
    private final EntityManager entityManager;

    // 채팅 결과 저장 트랜잭션과 분리: 실패해도 본 작업은 중단하지 않는다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Long messageId, String stage, Long durationMs) {
        entityManager.persist(new ChatAnalysisTrace(messageId, stage, durationMs));
    }
}

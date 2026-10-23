package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.common.enums.AnalysisStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메모리 작업이 유실된 경우 복구:
 * - 서버 재시작·대기열 초과로 처리되지 못한 PENDING
 * - 분석 도중 서버가 죽어 남은 PROCESSING
 * - Python 서버 중단·Gemini 일시 오류로 FAILED 된 최근 메시지 (재시도)
 *
 * 여러 Spring 서버가 동시에 실행해도 tryStart() 의 조건부 UPDATE 로 한 곳만 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAnalysisRecoveryJob {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatAnalysisStateService stateService;
    private final ChatAnalysisService chatAnalysisService;
    private final SpoilerApiProperties properties;

    @Scheduled(
            initialDelayString = "${app.spoiler.recovery.interval-ms:30000}",
            fixedDelayString = "${app.spoiler.recovery.interval-ms:30000}"
    )
    public void recover() {
        SpoilerApiProperties.Recovery recovery = properties.recovery();

        if (recovery == null || !recovery.enabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<ChatAnalysisTarget> targets = chatMessageRepository.findAnalysisTargets(
                MessageType.CHAT,
                AnalysisStatus.PENDING,
                now.minusSeconds(recovery.pendingAfterSeconds()),
                AnalysisStatus.PROCESSING,
                now.minusSeconds(recovery.processingAfterSeconds()),
                AnalysisStatus.FAILED,
                now.minusMinutes(recovery.retryFailedWithinMinutes()),
                PageRequest.of(0, recovery.batchSize())
        );

        if (targets.isEmpty()) {
            return;
        }

        log.info("[spoiler] 복구 대상 {}건", targets.size());

        for (ChatAnalysisTarget target : targets) {
            try {
                if (target.analysisStatus() == AnalysisStatus.PROCESSING) {
                    stateService.releaseStuck(target.messageId());
                }
                chatAnalysisService.analyze(target.toEvent());
            } catch (Exception e) {
                log.warn("[spoiler] 복구 분석 실패 messageId={}", target.messageId(), e);
            }
        }
    }
}

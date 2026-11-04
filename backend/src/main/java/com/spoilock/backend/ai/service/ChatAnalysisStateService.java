
package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.common.enums.AnalysisStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAnalysisStateService {

    private final ChatMessageRepository chatMessageRepository;
    private final SpoilerApiProperties properties;
    private final ObjectMapper objectMapper;

    // 분석 작업 선점
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryStart(Long messageId) {
        return chatMessageRepository.claimAnalysis(
                messageId,
                MessageType.CHAT,
                List.of(
                        AnalysisStatus.PENDING,
                        AnalysisStatus.FAILED
                ),
                AnalysisStatus.PROCESSING,
                LocalDateTime.now()
        ) == 1;
    }

    // 멈춘 작업 복구
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseStuck(Long messageId) {
        return chatMessageRepository.transitionAnalysisStatus(
                messageId,
                MessageType.CHAT,
                List.of(AnalysisStatus.PROCESSING),
                AnalysisStatus.PENDING
        ) == 1;
    }

    /**
     * 분석 상태 + 정책 코드 + 처리시간 저장
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean apply(
            Long messageId,
            AnalysisDecision decision,
            long durationMs,
            String rawVerdict
    ) {
        ChatMessage message =
                chatMessageRepository.findById(messageId)
                        .orElse(null);

        if (message == null
                || message.getAnalysisStatus()
                != AnalysisStatus.PROCESSING) {
            return false;
        }
        message.recordAiResponse(
                decision.aiResponse() == null
                ? null
                        : objectMapper.writeValueAsString(decision.aiResponse())
        );

        String outcome;

        if (decision.failed()) {
            message.failAnalysis(retryBaseDelaySeconds());
            outcome = "FAILED";
        } else {

            message.completeAnalysis(
                    decision.publish(),
                    decision.requiredEpisode(),
                    decision.untilFinale()
            );

            if (!decision.publish()) {
                outcome = "HELD";
            } else if (decision.requiredEpisode() != null) {
                outcome = "EPISODE_GATED";
            } else {
                outcome = "PUBLIC";
            }
        }

        String policyCode =
                AnalysisPolicyCode.from(decision).name();

        message.recordAnalysisMetrics(
                outcome,
                durationMs,
                policyCode,
                AnalysisPolicyDetail.from(decision),
                rawVerdict
        );

        return true;
    }


    private long retryBaseDelaySeconds() {
        SpoilerApiProperties.Recovery recovery = properties.recovery();
        return recovery == null ? 30 : recovery.retryBaseDelaySeconds();
    }
}


package com.spoilock.backend.ai.service;

import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.common.enums.AnalysisStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAnalysisStateService {

    private final ChatMessageRepository chatMessageRepository;

    // 분석 작업 선점
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryStart(Long messageId) {
        return chatMessageRepository.transitionAnalysisStatus(
                messageId,
                MessageType.CHAT,
                List.of(
                        AnalysisStatus.PENDING,
                        AnalysisStatus.FAILED
                ),
                AnalysisStatus.PROCESSING
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
            long durationMs
    ) {
        ChatMessage message =
                chatMessageRepository.findById(messageId)
                        .orElse(null);

        if (message == null
                || message.getAnalysisStatus()
                != AnalysisStatus.PROCESSING) {
            return false;
        }

        String outcome;

        if (decision.failed()) {

            message.failAnalysis();
            outcome = "FAILED";

        } else {

            // 기존 시청 진도 및 마지막 회차 공개 정책 유지
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

        // 정책 사유 코드
        String policyCode =
                AnalysisPolicyCode.from(decision).name();

        // 모니터링 데이터 저장
        message.recordAnalysisMetrics(
                outcome,
                durationMs,
                policyCode,
                AnalysisPolicyDetail.from(decision)
        );

        return true;
    }
}

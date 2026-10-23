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

/**
 * 분석 상태 저장 전용. 메서드마다 짧은 독립 트랜잭션(REQUIRES_NEW).
 * Python HTTP 호출 구간에는 DB 트랜잭션을 걸지 않기 위해 별도 Bean 으로 분리.
 */
@Service
@RequiredArgsConstructor
public class ChatAnalysisStateService {

    private final ChatMessageRepository chatMessageRepository;

    /** PENDING/FAILED → PROCESSING. 이미 다른 작업이 가져갔으면 false */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryStart(Long messageId) {
        return chatMessageRepository.transitionAnalysisStatus(
                messageId,
                MessageType.CHAT,
                List.of(AnalysisStatus.PENDING, AnalysisStatus.FAILED),
                AnalysisStatus.PROCESSING
        ) == 1;
    }

    /** 멈춘 PROCESSING → PENDING (복구용) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseStuck(Long messageId) {
        return chatMessageRepository.transitionAnalysisStatus(
                messageId,
                MessageType.CHAT,
                List.of(AnalysisStatus.PROCESSING),
                AnalysisStatus.PENDING
        ) == 1;
    }

    /** 분석 결정을 메시지 상태에 반영. PROCESSING 이 아니면(다른 작업이 처리함) 무시 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean apply(Long messageId, AnalysisDecision decision) {
        ChatMessage message = chatMessageRepository.findById(messageId).orElse(null);

        if (message == null
                || message.getAnalysisStatus() != AnalysisStatus.PROCESSING) {
            return false;
        }

        if (decision.failed()) {
            message.failAnalysis();
        } else {
            message.completeAnalysis(decision.publish(), decision.requiredEpisode());
        }

        return true;
    }
}

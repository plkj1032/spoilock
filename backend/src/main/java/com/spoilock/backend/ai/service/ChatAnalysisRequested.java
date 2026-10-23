package com.spoilock.backend.ai.service;

/**
 * 채팅 메시지 저장(커밋) 후 스포일러 분석을 요청하는 이벤트.
 * JPA 엔티티 대신 필요한 값만 담는다 (트랜잭션 종료 후 지연 로딩 문제 방지).
 */
public record ChatAnalysisRequested(
        Long messageId,
        Long roomId,
        Long dramaId,
        String content
) {
}

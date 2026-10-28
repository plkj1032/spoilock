package com.spoilock.backend.ai.service;

/**
 * 커뮤니티 게시글·댓글 저장(커밋) 후 스포일러 분석을 요청하는 이벤트.
 * JPA 엔티티 대신 필요한 값만 담는다 (트랜잭션 종료 후 지연 로딩 문제 방지).
 * 댓글의 dramaId 는 원글의 드라마.
 */
public record PostAnalysisRequested(
        Long postId,
        Long dramaId,
        String content
) {
}

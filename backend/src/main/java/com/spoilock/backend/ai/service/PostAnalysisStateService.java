package com.spoilock.backend.ai.service;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글·댓글 분석 상태 저장 전용. 메서드마다 짧은 독립 트랜잭션(REQUIRES_NEW).
 * Python HTTP 호출 구간에는 DB 트랜잭션을 걸지 않기 위해 별도 Bean 으로 분리.
 */
@Service
@RequiredArgsConstructor
public class PostAnalysisStateService {

    private final PostRepository postRepository;

    /** PENDING/FAILED → PROCESSING. 이미 다른 작업이 가져갔으면 false */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryStart(Long postId) {
        return postRepository.transitionAnalysisStatus(
                postId,
                List.of(AnalysisStatus.PENDING, AnalysisStatus.FAILED),
                AnalysisStatus.PROCESSING
        ) == 1;
    }

    /** 멈춘 PROCESSING → PENDING (복구용) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseStuck(Long postId) {
        return postRepository.transitionAnalysisStatus(
                postId,
                List.of(AnalysisStatus.PROCESSING),
                AnalysisStatus.PENDING
        ) == 1;
    }

    /** 분석 결정을 글 상태에 반영. PROCESSING 이 아니면(다른 작업이 처리함) 무시 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean apply(Long postId, AnalysisDecision decision) {
        Post post = postRepository.findById(postId).orElse(null);

        if (post == null
                || post.getAnalysisStatus() != AnalysisStatus.PROCESSING) {
            return false;
        }

        if (decision.failed()) {
            post.failAnalysis();
        } else {
            post.completeAnalysis(decision.publish(), decision.requiredEpisode());
        }

        return true;
    }
}

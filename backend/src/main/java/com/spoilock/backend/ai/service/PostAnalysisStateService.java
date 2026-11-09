
package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerApiProperties;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostAnalysisStateService {

    private final PostRepository postRepository;
    private final SpoilerApiProperties properties;

    // ============================================
    // 1. AI 분석 작업 선점
    // ============================================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryStart(Long postId, long revision) {

        return postRepository.claimAnalysis(
                postId,
                revision,
                List.of(
                        AnalysisStatus.PENDING,
                        AnalysisStatus.FAILED
                ),
                AnalysisStatus.PROCESSING,
                LocalDateTime.now(),
                List.of(
                        ModerationStatus.PENDING,
                        ModerationStatus.VISIBLE
                )
        ) == 1;
    }

    // ============================================
    // 2. 멈춘 AI 분석 작업 복구
    // ============================================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseStuck(Long postId, long revision) {

        return postRepository.transitionAnalysisStatus(
                postId,
                revision,
                List.of(AnalysisStatus.PROCESSING),
                AnalysisStatus.PENDING
        ) == 1;
    }

    // ============================================
    // 3. AI 분석 결과 저장
    // ============================================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean apply(
            Long postId,
            long revision,
            AnalysisDecision decision
    ) {
        Post post = postRepository
                .findByIdForUpdate(postId)
                .orElse(null);

        if (post == null
                || post.getContentRevision() != revision
                || post.getAnalysisStatus() != AnalysisStatus.PROCESSING
                || post.getModerationStatus() == ModerationStatus.REMOVED
                || post.getModerationStatus() == ModerationStatus.HIDDEN) {
            return false;
        }

        if (decision.failed()) {
            SpoilerApiProperties.Recovery recovery =
                    properties.recovery();

            post.failAnalysis(
                    recovery == null
                            ? 30
                            : recovery.retryBaseDelaySeconds()
            );
        } else {
            post.completeAnalysis(
                    decision.publish(),
                    decision.requiredEpisode(),
                    decision.untilFinale()
            );
        }

        return true;
    }
}

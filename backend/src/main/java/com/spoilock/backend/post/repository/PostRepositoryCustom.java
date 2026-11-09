
package com.spoilock.backend.post.repository;

import com.spoilock.backend.ai.service.PostAnalysisTarget;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.Post;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepositoryCustom {

    // 게시글 수정·삭제 시 비관적 락
    Optional<Post> findByIdForUpdate(Long postId);

    // 분석 상태 변경
    int transitionAnalysisStatus(
            Long postId,
            long revision,
            Collection<AnalysisStatus> from,
            AnalysisStatus to
    );

    // 분석 작업 선점
    int claimAnalysis(
            Long postId,
            long revision,
            Collection<AnalysisStatus> from,
            AnalysisStatus to,
            LocalDateTime now,
            Collection<ModerationStatus> allowed
    );

    // 분석 복구 대상 조회
    List<PostAnalysisTarget> findAnalysisTargets(
            Collection<ModerationStatus> moderation,
            AnalysisStatus pending,
            LocalDateTime pendingBefore,
            AnalysisStatus processing,
            LocalDateTime processingBefore,
            AnalysisStatus failed,
            int maxAttempts,
            LocalDateTime now,
            Pageable pageable
    );
}

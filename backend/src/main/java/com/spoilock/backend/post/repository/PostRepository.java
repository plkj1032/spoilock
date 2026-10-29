package com.spoilock.backend.post.repository;

import com.spoilock.backend.ai.service.PostAnalysisTarget;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByParentPost_IdAndModerationStatusIn(
            Long parentId,
            Collection<ModerationStatus> statuses,
            Pageable pageable
    );

    long countByParentPost_IdAndModerationStatusIn(
            Long parentId,
            Collection<ModerationStatus> statuses
    );

    long countByUser_IdAndParentPostIsNullAndModerationStatusIn(
            Long userId,
            Collection<ModerationStatus> statuses
    );



    // ============================================
    // 스포일러 분석 (게시글·댓글 공통)
    // ============================================

    /**
     * 분석 상태를 조건부로 변경 (원자적).
     * 예: PENDING/FAILED → PROCESSING. 다른 스레드·서버가 먼저 가져갔으면 0 반환.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post p
               set p.analysisStatus = :to
             where p.id = :postId
               and p.analysisStatus in :from
            """)
    int transitionAnalysisStatus(
            @Param("postId") Long postId,
            @Param("from") Collection<AnalysisStatus> from,
            @Param("to") AnalysisStatus to
    );

    /**
     * 복구 대상: 오래 남은 PENDING, 멈춘 PROCESSING, 최근 FAILED
     * (관리자가 숨김/삭제한 글은 제외)
     */
    @Query("""
            select new com.spoilock.backend.ai.service.PostAnalysisTarget(
                       p.id, d.id, p.content, p.analysisStatus)
              from Post p
              join p.drama d
             where p.moderationStatus in :moderation
               and (
                    (p.analysisStatus = :pending    and p.createdAt < :pendingBefore)
                 or (p.analysisStatus = :processing and p.createdAt < :processingBefore)
                 or (p.analysisStatus = :failed     and p.createdAt > :failedAfter)
               )
             order by p.id asc
            """)
    List<PostAnalysisTarget> findAnalysisTargets(
            @Param("moderation") Collection<ModerationStatus> moderation,
            @Param("pending") AnalysisStatus pending,
            @Param("pendingBefore") LocalDateTime pendingBefore,
            @Param("processing") AnalysisStatus processing,
            @Param("processingBefore") LocalDateTime processingBefore,
            @Param("failed") AnalysisStatus failed,
            @Param("failedAfter") LocalDateTime failedAfter,
            Pageable pageable
    );
}

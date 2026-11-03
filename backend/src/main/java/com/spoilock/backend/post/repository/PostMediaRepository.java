package com.spoilock.backend.post.repository;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.PostMedia;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostMediaRepository
        extends JpaRepository<PostMedia, Long> {

    long countByPost_Id(Long postId);

    List<PostMedia> findByPost_IdOrderBySortOrderAsc(
            Long postId
    );

    List<PostMedia> findTop100ByModerationStatusOrderByIdAsc(
            ModerationStatus status
    );

    /**
     * 판정 시작 (원자적): PENDING/FAILED → PROCESSING, 시도 횟수 +1.
     * 다른 스레드·서버가 먼저 가져갔으면 0.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PostMedia m
               set m.analysisStatus = :to,
                   m.analysisAttempts = m.analysisAttempts + 1,
                   m.analysisUpdatedAt = :now
             where m.id = :mediaId
               and m.analysisStatus in :from
            """)
    int startAnalysis(
            @Param("mediaId") Long mediaId,
            @Param("from") Collection<AnalysisStatus> from,
            @Param("to") AnalysisStatus to,
            @Param("now") LocalDateTime now
    );

    /** 멈춘 PROCESSING → PENDING (복구용) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PostMedia m
               set m.analysisStatus = :to
             where m.id = :mediaId
               and m.analysisStatus = :from
            """)
    int transitionAnalysisStatus(
            @Param("mediaId") Long mediaId,
            @Param("from") AnalysisStatus from,
            @Param("to") AnalysisStatus to
    );

    /**
     * 복구 대상: 오래 남은 PENDING, 멈춘 PROCESSING, 재시도 간격이 지난 FAILED (시도 상한 미만)
     */
    @Query("""
            select m.id
              from PostMedia m
             where m.moderationStatus = :visible
               and (
                    (m.analysisStatus = :pending    and m.createdAt < :pendingBefore)
                 or (m.analysisStatus = :processing and m.analysisUpdatedAt < :processingBefore)
                 or (m.analysisStatus = :failed     and m.analysisUpdatedAt < :failedBefore
                                                    and m.analysisAttempts < :maxAttempts)
               )
             order by m.id asc
            """)
    List<Long> findAnalysisTargets(
            @Param("visible") ModerationStatus visible,
            @Param("pending") AnalysisStatus pending,
            @Param("pendingBefore") LocalDateTime pendingBefore,
            @Param("processing") AnalysisStatus processing,
            @Param("processingBefore") LocalDateTime processingBefore,
            @Param("failed") AnalysisStatus failed,
            @Param("failedBefore") LocalDateTime failedBefore,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );
}

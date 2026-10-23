package com.spoilock.backend.chat.repository;

import com.spoilock.backend.ai.service.ChatAnalysisTarget;
import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.common.enums.AnalysisStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    /**
     * 분석 상태를 조건부로 변경 (원자적).
     * 예: PENDING/FAILED → PROCESSING. 다른 스레드·서버가 먼저 가져갔으면 0 반환.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChatMessage m
               set m.analysisStatus = :to
             where m.id = :messageId
               and m.messageType = :type
               and m.analysisStatus in :from
            """)
    int transitionAnalysisStatus(
            @Param("messageId") Long messageId,
            @Param("type") MessageType type,
            @Param("from") Collection<AnalysisStatus> from,
            @Param("to") AnalysisStatus to
    );

    /**
     * 복구 대상: 오래 남은 PENDING, 멈춘 PROCESSING, 최근 FAILED
     */
    @Query("""
            select new com.spoilock.backend.ai.service.ChatAnalysisTarget(
                       m.id, r.id, d.id, m.content, m.analysisStatus)
              from ChatMessage m
              join m.room r
              join r.drama d
             where m.messageType = :type
               and (
                    (m.analysisStatus = :pending    and m.createdAt < :pendingBefore)
                 or (m.analysisStatus = :processing and m.createdAt < :processingBefore)
                 or (m.analysisStatus = :failed     and m.createdAt > :failedAfter)
               )
             order by m.id asc
            """)
    List<ChatAnalysisTarget> findAnalysisTargets(
            @Param("type") MessageType type,
            @Param("pending") AnalysisStatus pending,
            @Param("pendingBefore") LocalDateTime pendingBefore,
            @Param("processing") AnalysisStatus processing,
            @Param("processingBefore") LocalDateTime processingBefore,
            @Param("failed") AnalysisStatus failed,
            @Param("failedAfter") LocalDateTime failedAfter,
            Pageable pageable
    );
}

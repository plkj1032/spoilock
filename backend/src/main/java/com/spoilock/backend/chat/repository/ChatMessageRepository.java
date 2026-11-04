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
import com.spoilock.backend.ai.service.ChatContextRow;
import com.spoilock.backend.common.enums.ModerationStatus;

import java.util.Optional;
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
     * 분석 선점: PENDING/FAILED → PROCESSING + 시작 시각 기록 (원자적). 다른 서버가 먼저 가져갔으면 0
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChatMessage m
               set m.analysisStatus = :to,
                   m.analysisStartedAt = :now
             where m.id = :messageId
               and m.messageType = :type
               and m.analysisStatus in :from
            """)
    int claimAnalysis(
            @Param("messageId") Long messageId,
            @Param("type") MessageType type,
            @Param("from") Collection<AnalysisStatus> from,
            @Param("to") AnalysisStatus to,
            @Param("now") LocalDateTime now
    );

    /**
     * 복구 대상: 오래 남은 PENDING, 멈춘 PROCESSING, 재시도 시각이 된 FAILED(최대 횟수 미만)
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
                 or (m.analysisStatus = :processing
                     and coalesce(m.analysisStartedAt, m.createdAt) < :processingBefore)
                 or (m.analysisStatus = :failed
                     and m.analysisAttempts < :maxAttempts
                     and (m.analysisRetryAt is null or m.analysisRetryAt <= :now))
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
            @Param("maxAttempts") int maxAttempts,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    /**
     * 스포 판정 맥락용: 메시지 1건의 작성자·작성 시각
     */
    @Query("""
            select new com.spoilock.backend.ai.service.ChatContextRow(
                       m.id, m.user.id, m.content, m.createdAt)
              from ChatMessage m
             where m.id = :messageId
            """)
    Optional<ChatContextRow> findContextAnchor(@Param("messageId") Long messageId);

    /**
     * 스포 판정 맥락용: 같은 방에서 이 메시지보다 앞선 채팅 (최근 것부터). 삭제된 메시지는 제외
     */
    @Query("""
            select new com.spoilock.backend.ai.service.ChatContextRow(
                       m.id, m.user.id, m.content, m.createdAt)
              from ChatMessage m
             where m.room.id = :roomId
               and m.id < :beforeId
               and m.messageType = :type
               and m.moderationStatus <> :removed
               and m.createdAt >= :since
             order by m.id desc
            """)
    List<ChatContextRow> findContextBefore(
            @Param("roomId") Long roomId,
            @Param("beforeId") Long beforeId,
            @Param("type") MessageType type,
            @Param("removed") ModerationStatus removed,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );
}
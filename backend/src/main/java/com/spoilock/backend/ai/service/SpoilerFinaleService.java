package com.spoilock.backend.ai.service;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * '드라마 마지막 회차 기준'으로 공개한 글 관리.
 *
 * - finaleEpisode: 작품의 마지막 회차 = max(관리자가 입력한 전체 회차 수, 등록된 회차 번호)
 *   (회차 등록이 방영보다 느려도 끝까지 본 사람만 보도록, 분석된 회차가 아니라 전체 회차 수 기준)
 * - syncDrama:    작품의 회차 수가 바뀌면 표시된 글의 공개 회차를 새 마지막 회차로 변경
 * - requeueRecent: 작품에 새 사건이 등록·승인·분석되면 최근 표시 글을 다시 판정 대기로 → 복구 작업이 재판정
 */
@Slf4j
@Service
public class SpoilerFinaleService {

    /** 새 사건 등록 시 다시 판정할 범위 (오래된 글까지 재판정하면 Gemini 비용이 커짐) */
    static final long REJUDGE_DAYS = 7;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Integer finaleEpisode(Long dramaId) {
        if (dramaId == null) {
            return null;
        }
        Integer total = first(entityManager.createQuery(
                        "select d.totalEpisodeCount from Drama d where d.id = :id", Integer.class)
                .setParameter("id", dramaId)
                .getResultList());
        Integer lastRegistered = first(entityManager.createQuery(
                        "select max(e.episodeNumber) from Episode e where e.drama.id = :id", Integer.class)
                .setParameter("id", dramaId)
                .getResultList());

        int finale = Math.max(total == null ? 0 : total, lastRegistered == null ? 0 : lastRegistered);
        return finale > 0 ? finale : null;
    }

    /** 작품 회차 수 변경 → 마지막 회차 기준 글의 공개 회차 갱신. 호출한 트랜잭션 안에서 실행 */
    @Transactional
    public int syncDrama(Long dramaId) {
        entityManager.flush();
        Integer finale = finaleEpisode(dramaId);
        if (finale == null) {
            return 0;
        }

        int chats = entityManager.createQuery("""
                        update ChatMessage m
                           set m.requiredEpisodeNumber = :finale
                         where m.requiredUntilFinale = true
                           and m.room.id in (select r.id from ChatRoom r where r.drama.id = :dramaId)
                        """)
                .setParameter("finale", finale)
                .setParameter("dramaId", dramaId)
                .executeUpdate();

        int posts = entityManager.createQuery("""
                        update Post p
                           set p.requiredEpisodeNumber = :finale
                         where p.requiredUntilFinale = true
                           and p.drama.id = :dramaId
                        """)
                .setParameter("finale", finale)
                .setParameter("dramaId", dramaId)
                .executeUpdate();

        if (chats + posts > 0) {
            log.info("[spoiler] 마지막 회차 기준 글 갱신 dramaId={} → {}화 (채팅 {}, 게시글 {})",
                    dramaId, finale, chats, posts);
        }
        return chats + posts;
    }

    /**
     * 새 사건이 생긴 작품의 최근 '마지막 회차 기준' 글을 다시 판정 대기로.
     * 판정 전까지는 잠시 가려지고, 복구 작업(30초 주기)이 다시 판정한다.
     * 실패해도 사건 관리 기능에는 영향을 주지 않도록 예외를 삼킨다.
     */
    @Transactional
    public int requeueRecent(Long dramaId) {
        if (dramaId == null) {
            return 0;
        }
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(REJUDGE_DAYS);
            List<ModerationStatus> moderated = List.of(ModerationStatus.HIDDEN, ModerationStatus.REMOVED);

            int chats = entityManager.createQuery("""
                            update ChatMessage m
                               set m.analysisStatus = :pending
                             where m.requiredUntilFinale = true
                               and m.analysisStatus = :completed
                               and m.moderationStatus not in :moderated
                               and m.createdAt > :since
                               and m.room.id in (select r.id from ChatRoom r where r.drama.id = :dramaId)
                            """)
                    .setParameter("pending", AnalysisStatus.PENDING)
                    .setParameter("completed", AnalysisStatus.COMPLETED)
                    .setParameter("moderated", moderated)
                    .setParameter("since", since)
                    .setParameter("dramaId", dramaId)
                    .executeUpdate();

            int posts = entityManager.createQuery("""
                            update Post p
                               set p.analysisStatus = :pending
                             where p.requiredUntilFinale = true
                               and p.analysisStatus = :completed
                               and p.moderationStatus not in :moderated
                               and p.createdAt > :since
                               and p.drama.id = :dramaId
                            """)
                    .setParameter("pending", AnalysisStatus.PENDING)
                    .setParameter("completed", AnalysisStatus.COMPLETED)
                    .setParameter("moderated", moderated)
                    .setParameter("since", since)
                    .setParameter("dramaId", dramaId)
                    .executeUpdate();

            if (chats + posts > 0) {
                log.info("[spoiler] 새 사건 등록 → 마지막 회차 기준 글 재판정 대기 dramaId={} (채팅 {}, 게시글 {})",
                        dramaId, chats, posts);
            }
            return chats + posts;

        } catch (Exception e) {
            log.warn("[spoiler] 재판정 대기 전환 실패 dramaId={}: {}", dramaId, e.toString());
            return 0;
        }
    }

    private static Integer first(List<Integer> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }
}

package com.spoilock.backend.ai.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 글 작성자의 시청 진도 (추측성 글 판단용).
 *
 * 판정은 글 저장 직후에 실행되므로 '보낸 시점'의 진도와 거의 같다.
 * 복구 작업이 나중에 다시 판정하면 그 사이 진도가 올라 있을 수 있는데,
 * 진도가 높을수록 더 많이 가리는 쪽이라 안전하다.
 *
 * 반환: 시청 회차 (진도 기록이 없으면 0, 조회 실패면 null → 추측은 모르고 한 것으로 처리)
 */
@Slf4j
@Service
public class AuthorProgressService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Integer forChatMessage(Long messageId, Long dramaId) {
        return find("""
                select up.watchedEpisodeNumber
                  from ChatMessage m, UserProgress up
                 where m.id = :id
                   and up.user = m.user
                   and up.drama.id = :dramaId
                """, messageId, dramaId);
    }

    /** 게시글·댓글 (댓글의 dramaId 는 원글의 작품) */
    @Transactional(readOnly = true)
    public Integer forPost(Long postId, Long dramaId) {
        return find("""
                select up.watchedEpisodeNumber
                  from Post p, UserProgress up
                 where p.id = :id
                   and up.user = p.user
                   and up.drama.id = :dramaId
                """, postId, dramaId);
    }

    private Integer find(String jpql, Long id, Long dramaId) {
        if (id == null || dramaId == null) {
            return null;
        }
        try {
            List<Integer> rows = entityManager.createQuery(jpql, Integer.class)
                    .setParameter("id", id)
                    .setParameter("dramaId", dramaId)
                    .setMaxResults(1)
                    .getResultList();
            return rows.isEmpty() || rows.get(0) == null ? 0 : rows.get(0);
        } catch (Exception e) {
            log.warn("[spoiler] 작성자 시청 진도 조회 실패 id={} dramaId={}: {}", id, dramaId, e.toString());
            return null;
        }
    }
}

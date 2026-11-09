
package com.spoilock.backend.post.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.spoilock.backend.ai.service.PostAnalysisTarget;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.Post;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.spoilock.backend.post.entity.QPost.post;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl
        implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    // ============================================
    // 1. 수정·삭제 대상 조회 및 비관적 락
    // ============================================
    @Override
    @Transactional
    public Optional<Post> findByIdForUpdate(Long postId) {

        Post result = queryFactory
                .selectFrom(post)
                .where(
                        post.id.eq(postId)
                )
                .setLockMode(
                        LockModeType.PESSIMISTIC_WRITE
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    // ============================================
    // 2. 분석 상태 변경
    // ============================================
    @Override
    @Transactional
    public int transitionAnalysisStatus(
            Long postId,
            long revision,
            Collection<AnalysisStatus> from,
            AnalysisStatus to
    ) {
        if (from == null || from.isEmpty()) {
            return 0;
        }

        entityManager.flush();

        long affected = queryFactory
                .update(post)
                .set(post.analysisStatus, to)
                .where(
                        post.id.eq(postId),
                        post.contentRevision.eq(revision),
                        post.analysisStatus.in(from)
                )
                .execute();

        entityManager.clear();

        return Math.toIntExact(affected);
    }

    // ============================================
    // 3. 분석 작업 선점
    // PENDING / FAILED -> PROCESSING
    // ============================================
    @Override
    @Transactional
    public int claimAnalysis(
            Long postId,
            long revision,
            Collection<AnalysisStatus> from,
            AnalysisStatus to,
            LocalDateTime now,
            Collection<ModerationStatus> allowed
    ) {
        if (from == null || from.isEmpty()
                || allowed == null || allowed.isEmpty()) {
            return 0;
        }

        entityManager.flush();

        long affected = queryFactory
                .update(post)
                .set(post.analysisStatus, to)
                .set(post.analysisStartedAt, now)
                .where(
                        post.id.eq(postId),
                        post.contentRevision.eq(revision),
                        post.analysisStatus.in(from),
                        post.moderationStatus.in(allowed)
                )
                .execute();

        entityManager.clear();

        return Math.toIntExact(affected);
    }

    // ============================================
    // 4. 스포일러 분석 복구 대상 조회
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public List<PostAnalysisTarget> findAnalysisTargets(
            Collection<ModerationStatus> moderation,
            AnalysisStatus pending,
            LocalDateTime pendingBefore,
            AnalysisStatus processing,
            LocalDateTime processingBefore,
            AnalysisStatus failed,
            int maxAttempts,
            LocalDateTime now,
            Pageable pageable
    ) {
        if (moderation == null || moderation.isEmpty()) {
            return List.of();
        }

        // 1) 오래된 PENDING 상태
        BooleanExpression pendingCondition =
                post.analysisStatus.eq(pending)
                        .and(
                                post.createdAt.lt(pendingBefore)
                        );

        // 2) 처리 시간이 오래 걸려 멈춘 PROCESSING
        // analysisStartedAt이 없으면 createdAt 기준
        BooleanExpression stuckCondition =
                post.analysisStartedAt.isNotNull()
                        .and(
                                post.analysisStartedAt.lt(
                                        processingBefore
                                )
                        )
                        .or(
                                post.analysisStartedAt.isNull()
                                        .and(
                                                post.createdAt.lt(
                                                        processingBefore
                                                )
                                        )
                        );

        BooleanExpression processingCondition =
                post.analysisStatus.eq(processing)
                        .and(stuckCondition);

        // 3) 재시도 시각이 지난 FAILED
        BooleanExpression retryCondition =
                post.analysisRetryAt.isNull()
                        .or(
                                post.analysisRetryAt.loe(now)
                        );

        BooleanExpression failedCondition =
                post.analysisStatus.eq(failed)
                        .and(
                                post.analysisAttempts.lt(
                                        maxAttempts
                                )
                        )
                        .and(retryCondition);

        // 복구 대상 세 조건 결합
        BooleanExpression targetCondition =
                pendingCondition
                        .or(processingCondition)
                        .or(failedCondition);

        JPAQuery<PostAnalysisTarget> query = queryFactory
                .select(
                        Projections.constructor(
                                PostAnalysisTarget.class,
                                post.id,
                                post.drama.id,
                                post.content,
                                post.analysisStatus,
                                post.contentRevision
                        )
                )
                .from(post)
                .where(
                        post.moderationStatus.in(moderation),
                        targetCondition
                )
                .orderBy(post.id.asc());

        // 페이지 제한
        if (pageable != null && pageable.isPaged()) {
            query
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize());
        }

        return query.fetch();
    }
}

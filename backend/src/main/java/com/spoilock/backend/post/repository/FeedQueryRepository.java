
package com.spoilock.backend.post.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.user.enums.AccountStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.spoilock.backend.post.entity.QPost.post;
import static com.spoilock.backend.follow.entity.QUserFollow.userFollow;

@Repository
@RequiredArgsConstructor
public class FeedQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Post> findFeed(
            Long viewerId,
            String type,
            LocalDateTime cursorTime,
            Long cursorId,
            int limit,
            Long dramaId
    ) {
        // 전체 피드인지 팔로잉 피드인지
        BooleanExpression followingCondition = null;

        if ("FOLLOWING".equals(type)) {
            followingCondition =
                    post.user.id.eq(viewerId)
                            .or(
                                    JPAExpressions.selectOne()
                                            .from(userFollow)
                                            .where(
                                                    userFollow.follower.id.eq(viewerId),
                                                    userFollow.following.id.eq(post.user.id)
                                            )
                                            .exists()
                            );
        }

        // 마지막으로 조회한 게시글보다 과거 게시글만 조회
        BooleanExpression cursorCondition = null;

        if (cursorTime != null && cursorId != null) {
            cursorCondition =
                    post.createdAt.lt(cursorTime)
                            .or(
                                    post.createdAt.eq(cursorTime)
                                            .and(post.id.lt(cursorId))
                            );
        }

        return queryFactory
                .selectFrom(post)
                .join(post.user).fetchJoin()
                .join(post.drama).fetchJoin()
                .where(
                        // 댓글은 피드에서 제외
                        post.parentPost.isNull(),

                        // 정상 사용자 및 공개 작품
                        post.user.accountStatus.eq(AccountStatus.ACTIVE),
                        post.drama.status.eq(DramaStatus.PUBLISHED),

                        // 관리자 숨김/삭제 게시글 제외
                        post.moderationStatus.in(
                                ModerationStatus.PENDING,
                                ModerationStatus.VISIBLE
                        ),

                        followingCondition,
                        cursorCondition,
                        dramaId == null ? null : post.drama.id.eq(dramaId)
                )
                .orderBy(
                        post.createdAt.desc(),
                        post.id.desc()
                )
                .limit(limit)
                .fetch();
    }
    public List<Post> findUserFeed(
            Long authorId,
            LocalDateTime cursorTime,
            Long cursorId,
            int limit
    ) {
        BooleanExpression cursorCondition = null;

        if (cursorTime != null && cursorId != null) {
            cursorCondition = post.createdAt.lt(cursorTime)
                    .or(
                            post.createdAt.eq(cursorTime)
                                    .and(post.id.lt(cursorId))
                    );
        }

        return queryFactory
                .selectFrom(post)
                .join(post.user).fetchJoin()
                .join(post.drama).fetchJoin()
                .where(
                        post.user.id.eq(authorId),
                        post.parentPost.isNull(),
                        post.user.accountStatus.eq(
                                AccountStatus.ACTIVE
                        ),
                        post.drama.status.eq(
                                DramaStatus.PUBLISHED
                        ),
                        post.moderationStatus.in(
                                ModerationStatus.PENDING,
                                ModerationStatus.VISIBLE
                        ),
                        cursorCondition
                )
                .orderBy(
                        post.createdAt.desc(),
                        post.id.desc()
                )
                .limit(limit)
                .fetch();
    }
    public long countFeed(
            Long viewerId,
            String type,
            Long dramaId
    ) {
        BooleanExpression followingCondition = null;

        if ("FOLLOWING".equals(type)) {
            followingCondition =
                    post.user.id.eq(viewerId)
                            .or(
                                    JPAExpressions.selectOne()
                                            .from(userFollow)
                                            .where(
                                                    userFollow.follower.id.eq(viewerId),
                                                    userFollow.following.id.eq(post.user.id)
                                            )
                                            .exists()
                            );
        }

        Long count = queryFactory
                .select(post.id.count())
                .from(post)
                .where(
                        post.parentPost.isNull(),

                        post.user.accountStatus.eq(AccountStatus.ACTIVE),
                        post.drama.status.eq(DramaStatus.PUBLISHED),

                        post.moderationStatus.in(
                                ModerationStatus.PENDING,
                                ModerationStatus.VISIBLE
                        ),

                        followingCondition,

                        dramaId == null
                                ? null
                                : post.drama.id.eq(dramaId)
                )
                .fetchOne();

        return count == null ? 0L : count;
    }
}

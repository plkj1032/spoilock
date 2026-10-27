package com.spoilock.backend.post.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.post.entity.Post;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.spoilock.backend.post.entity.QPost.post;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 게시글 미디어 업로드 시 동시 수정 방지
    // 반드시 활성 트랜잭션 내부에서 호출
    public Optional<Post> findForMediaUpload(Long postId)
    {
        Post result = queryFactory
                .selectFrom(post)
                .where(post.id.eq(postId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        return Optional.ofNullable(result);
    }

}

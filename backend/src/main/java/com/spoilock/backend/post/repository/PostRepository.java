
package com.spoilock.backend.post.repository;

import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface PostRepository
        extends JpaRepository<Post, Long>, PostRepositoryCustom {

    // 특정 게시글의 댓글 조회
    Page<Post> findByParentPost_IdAndModerationStatusIn(
            Long parentId,
            Collection<ModerationStatus> statuses,
            Pageable pageable
    );

    // 특정 게시글의 댓글 수
    long countByParentPost_IdAndModerationStatusIn(
            Long parentId,
            Collection<ModerationStatus> statuses
    );

    // 사용자별 게시글 수
    long countByUser_IdAndParentPostIsNullAndModerationStatusIn(
            Long userId,
            Collection<ModerationStatus> statuses
    );
}

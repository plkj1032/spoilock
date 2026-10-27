package com.spoilock.backend.post.repository;

import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;

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


}

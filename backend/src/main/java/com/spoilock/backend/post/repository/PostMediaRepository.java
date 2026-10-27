package com.spoilock.backend.post.repository;

import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
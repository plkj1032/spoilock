package com.spoilock.backend.post.repository;

import com.spoilock.backend.post.entity.PostLike;
import com.spoilock.backend.post.entity.id.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository
    extends JpaRepository<PostLike, PostLikeId> {

    long countByPost_Id(Long postId);

}

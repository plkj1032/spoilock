package com.spoilock.backend.follow.repository;

import com.spoilock.backend.follow.entity.UserFollow;
import com.spoilock.backend.follow.entity.id.UserFollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFollowRepository
    extends JpaRepository<UserFollow, UserFollowId> {

    // A가 B를 팔로우하고 있는지
    boolean existsByFollower_IdAndFollowing_Id(
            Long followerId,
            Long followingId
    );

    // 특정 사용자의 팔로워 수
    long countByFollowing_Id(Long followingId);

    // 특정 사용자의 팔로잉 수
    long countByFollower_id(Long followerId);

    @EntityGraph(attributePaths = "follower")
    Page<UserFollow> findByFollowing_Id(
            Long followingId,
            Pageable pageable
    );

    // 내가 팔로우하는 사용자 목록
    @EntityGraph(attributePaths = "following")
    Page<UserFollow> findByFollower_Id(
            Long followerId,
            Pageable pageable
    );

}

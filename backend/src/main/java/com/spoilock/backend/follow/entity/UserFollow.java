package com.spoilock.backend.follow.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.follow.entity.id.UserFollowId;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFollow extends CreatedAtEntity {

    @EmbeddedId
    private UserFollowId id = new UserFollowId();

    // 팔로우를 하는 사용자
    @MapsId("followerId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "follower_id",
            nullable = false
    )
    private User follower;

    // 팔로우를 받는 사용자
    @MapsId("followingId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "following_id",
            nullable = false
    )
    private User following;
}

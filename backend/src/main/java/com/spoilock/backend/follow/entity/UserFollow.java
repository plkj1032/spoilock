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
    private UserFollowId id;

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

    // 팔로우 관계 생성
    public static UserFollow create(
            User follower,
            User following
    )
    {
        if(follower == null || following == null)
        {
            throw new IllegalArgumentException(
                    "팔로우 사용자 정보가 필요합니다."
            );
        }

        if(follower.getId() == null || following.getId() == null)
        {
            throw new IllegalArgumentException(
                    "자기 자신을 팔로우할 수 없습니다."
            );
        }

        UserFollow follow = new UserFollow();

        follow.id = new UserFollowId(
                follower.getId(),
                following.getId()
        );

        follow.follower = follower;
        follow.following = following;

        return follow;
    }
}

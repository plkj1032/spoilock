package com.spoilock.backend.follow.entity.id;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserFollowId implements Serializable {

    @Column(name = "follower_id")
    private Long followerId;

    @Column(name = "following_id")
    private Long followingId;
}

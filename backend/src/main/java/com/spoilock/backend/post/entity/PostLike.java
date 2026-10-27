package com.spoilock.backend.post.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.post.entity.id.PostLikeId;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends CreatedAtEntity {

    @EmbeddedId
    private PostLikeId id = new PostLikeId();

    @MapsId("userId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @MapsId("postId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "post_id",
            nullable = false
    )
    private Post post;

    public static PostLike create(User user, Post post)
    {
        PostLike like = new PostLike();

        like.id = new PostLikeId(user.getId(), post.getId());
        like.user = user;
        like.post = post;

        return like;
    }
}

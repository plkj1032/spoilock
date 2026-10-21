package com.spoilock.backend.post.entity.id;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PostLikeId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "post_id")
    private Long postId;
}

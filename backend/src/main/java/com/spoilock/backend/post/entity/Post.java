package com.spoilock.backend.post.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.entity.Drama;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "drama_id",
            nullable = false
    )
    private Drama drama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_post_id")
    private Post parentPost;

    @Column(length = 200)
    private String title;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Column(name = "required_episode_number")
    private Integer requiredEpisodeNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "analysis_status",
            nullable = false,
            length = 20
    )
    private AnalysisStatus analysisStatus =
            AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "moderation_status",
            nullable = false,
            length = 20
    )
    private ModerationStatus moderationStatus =
            ModerationStatus.PENDING;

    // SNS 피드 작성
    public static Post create(
            User author,
            Drama drama,
            String content
    )
    {
        Post post = new Post();

        post.user = author;
        post.drama = drama;
        post.content = content.trim();

        // 아직 AI 검열이 끝나지 않은 글
        post.analysisStatus = AnalysisStatus.PENDING;
        post.moderationStatus = ModerationStatus.PENDING;

        return post;
    }

    public static Post createComment(
            User author,
            Post parent,
            String content
    )
    {
        if(parent.getParentPost() != null)
        {
            throw new IllegalArgumentException(
                    "대댓글은 아직 지원하지 않습니다."
            );
        }

        Post comment = new Post();
        comment.user = author;
        comment.drama = parent.getDrama();
        comment.parentPost = parent;
        comment.content = content.trim();
        comment.analysisStatus = AnalysisStatus.PENDING;
        comment.moderationStatus = ModerationStatus.PENDING;

        return comment;
    }
}

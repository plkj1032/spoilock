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

    // ============================================
    // 스포일러 분석 결과 반영 (ChatMessage 와 같은 규칙)
    // ============================================

    /**
     * 분석 완료.
     * publish=true  → 공개 (requiredEpisodeNumber=null 이면 전원, 값이 있으면 그 회차 이상 시청자)
     * publish=false → 검토 대기 (원문 차단)
     */
    public void completeAnalysis(
            boolean publish,
            Integer requiredEpisodeNumber
    )
    {
        this.analysisStatus = AnalysisStatus.COMPLETED;
        this.requiredEpisodeNumber = publish ? requiredEpisodeNumber : null;

        // 관리자가 이미 숨김/삭제한 글은 분석 결과로 다시 공개하지 않음
        if (isModeratedByAdmin()) {
            return;
        }

        this.moderationStatus = publish
                ? ModerationStatus.VISIBLE
                : ModerationStatus.PENDING;
    }

    /** 분석 실패: 원문 차단 유지, 복구 작업이 재시도 */
    public void failAnalysis()
    {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.requiredEpisodeNumber = null;

        if (!isModeratedByAdmin()) {
            this.moderationStatus = ModerationStatus.PENDING;
        }
    }

    private boolean isModeratedByAdmin()
    {
        return moderationStatus == ModerationStatus.HIDDEN
                || moderationStatus == ModerationStatus.REMOVED;
    }
}

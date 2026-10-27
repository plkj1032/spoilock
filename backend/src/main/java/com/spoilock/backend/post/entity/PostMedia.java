package com.spoilock.backend.post.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.common.enums.ModerationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMedia extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "storage_key", nullable = false, length = 80)
    private String storageKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "media_type", nullable = false, length = 30)
    private String mediaType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus =
            ModerationStatus.PENDING;

    @Column(name = "required_episode_number")
    private Integer requiredEpisodeNumber;

    public static PostMedia create(
            Post post,
            String storageKey,
            String filename,
            String mediaType,
            long sizeBytes,
            int sortOrder
    ) {
        PostMedia media = new PostMedia();

        media.post = post;
        media.storageKey = storageKey;
        media.originalFilename = filename;
        media.mediaType = mediaType;
        media.sizeBytes = sizeBytes;
        media.sortOrder = sortOrder;

        return media;
    }

    public void review(boolean approved, Integer requiredEpisode) {
        this.moderationStatus = approved
                ? ModerationStatus.VISIBLE
                : ModerationStatus.REMOVED;

        this.requiredEpisodeNumber =
                approved ? requiredEpisode : null;
    }
}
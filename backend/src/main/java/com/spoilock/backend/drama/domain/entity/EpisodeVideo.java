
package com.spoilock.backend.drama.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "episode_videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodeVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "episode_id",
            nullable = false,
            unique = true
    )
    private Episode episode;

    @Column(
            name = "original_filename",
            nullable = false,
            length = 255
    )
    private String originalFilename;

    @Column(
            name = "storage_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @CreationTimestamp
    @Column(
            name = "uploaded_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime uploadedAt;

    public static EpisodeVideo create(
            Episode episode,
            String originalFilename,
            String storageKey,
            long sizeBytes
    ) {
        EpisodeVideo video = new EpisodeVideo();

        video.episode = episode;
        video.originalFilename = originalFilename;
        video.storageKey = storageKey;
        video.sizeBytes = sizeBytes;

        return video;
    }
}

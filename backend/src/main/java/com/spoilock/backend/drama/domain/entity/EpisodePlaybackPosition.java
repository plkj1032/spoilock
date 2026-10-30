package com.spoilock.backend.drama.domain.entity;

import com.spoilock.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "episode_playback_positions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_playback_user_episode",
                        columnNames = {"user_id", "episode_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodePlaybackPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "position_seconds", nullable = false)
    private double positionSeconds;

    @Column(name = "duration_seconds", nullable = false)
    private double durationSeconds;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static EpisodePlaybackPosition create(
            User user,
            Episode episode,
            double positionSeconds,
            double durationSeconds
    ) {
        EpisodePlaybackPosition playbackPosition =
                new EpisodePlaybackPosition();

        playbackPosition.user = user;
        playbackPosition.episode = episode;
        playbackPosition.updatePosition(
                positionSeconds,
                durationSeconds
        );

        return playbackPosition;
    }

    public void updatePosition(
            double positionSeconds,
            double durationSeconds
    ) {
        if (!Double.isFinite(positionSeconds)
                || !Double.isFinite(durationSeconds)
                || durationSeconds <= 0
                || positionSeconds < 0
                || positionSeconds > durationSeconds) {
            throw new IllegalArgumentException(
                    "올바르지 않은 영상 재생 위치입니다."
            );
        }

        this.positionSeconds = positionSeconds;
        this.durationSeconds = durationSeconds;
        this.updatedAt = LocalDateTime.now();
    }
}
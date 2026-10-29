package com.spoilock.backend.drama.domain.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "episodes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_episode_drama_number",
                        columnNames = {"drama_id", "episode_number"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Episode extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "drama_id",
            nullable = false
    )
    private Drama drama;

    @Column(
            name = "episode_number",
            nullable = false
    )
    private Integer episodeNumber;

    @Column(length = 200)
    private String title;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    public static Episode create(
            Drama drama,
            Integer episodeNumber,
            String title
    )
    {
        Episode episode = new Episode();

        episode.drama = drama;
        episode.episodeNumber = episodeNumber;
        episode.title = title;

        return episode;
    }
}

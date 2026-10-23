package com.spoilock.backend.drama.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.drama.enums.DramaStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dramas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drama extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "poster_url", length = 1024)
    private String posterUrl;

    @Column(name = "total_episode_count")
    private Integer totalEpisodeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DramaStatus status = DramaStatus.DRAFT;

    public static Drama createDraft(
            String title,
            String synopsis,
            String posterUrl,
            Integer totalEpisodeCount
    )
    {
        Drama drama = new Drama();

        drama.title = title;
        drama.synopsis = synopsis;
        drama.posterUrl = posterUrl;
        drama.totalEpisodeCount = totalEpisodeCount;
        drama.status = DramaStatus.DRAFT;

        return drama;
    }

    public void publish()
    {
        this.status = DramaStatus.PUBLISHED;
    }

    public void updateDraft(
            String title,
            String synopsis,
            String posterUrl,
            Integer totalEpisodeCount
    )
    {
        this.title = title;
        this.synopsis = synopsis;
        this.posterUrl = posterUrl;
        this.totalEpisodeCount = totalEpisodeCount;
    }

    public void hide()
    {
        this.status = DramaStatus.HIDDEN;
    }
}

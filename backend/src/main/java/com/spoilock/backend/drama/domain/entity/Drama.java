package com.spoilock.backend.drama.domain.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.drama.domain.enums.DramaGenre;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "drama_genres",
            joinColumns = @JoinColumn(name = "drama_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 30)
    private Set<DramaGenre> genres = new HashSet<>();

    @Column(name = "premiere_date")
    private LocalDate premiereDate;

    @Column(name = "directors", length = 500)
    private String directors;

    @Column(name = "writers", length = 500)
    private String writers;

    @Column(name = "cast_names", length = 2000)
    private String castNames;

    @Column(name = "official_homepage_url", length = 1000)
    private String officialHomepageUrl;

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

    public void updateMetadata(
            Set<DramaGenre> genres,
            LocalDate premiereDate,
            String directors,
            String writers,
            String castNames
    ) {
        Set<DramaGenre> selectedGenres =
                genres == null ? new HashSet<>() : new HashSet<>(genres);

        if (selectedGenres.contains(null)) {
            throw new IllegalArgumentException(
                    "장르에는 null을 포함할 수 없습니다."
            );
        }

        String normalizedDirectors =
                normalizeMetadata(directors, 500, "감독");

        String normalizedWriters =
                normalizeMetadata(writers, 500, "작가");

        String normalizedCastNames =
                normalizeMetadata(castNames, 2000, "배우");

        this.genres.clear();
        this.genres.addAll(selectedGenres);

        this.premiereDate = premiereDate;
        this.directors = normalizedDirectors;
        this.writers = normalizedWriters;
        this.castNames = normalizedCastNames;
    }

    private static String normalizeMetadata(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "은 " + maxLength + "자 이하여야 합니다."
            );
        }

        return normalized;
    }
    public void updateOfficialHomepageUrl(String officialHomepageUrl) {
        this.officialHomepageUrl = normalizeMetadata(
                officialHomepageUrl,
                1000,
                "공식 홈페이지 주소"
        );
    }
}

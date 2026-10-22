package com.spoilock.backend.drama.entity;

import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_progress_user_drama",
                        columnNames = {"user_id", "drama_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProgress {

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

    @Column(
            name = "watched_episode_number",
            nullable = false
    )
    private Integer watchedEpisodeNumber = 0;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    // 최초 시청 진도 생성
    public static UserProgress create(
            User user,
            Drama drama,
            Integer watchedEpisodeNumber
    ) {
        UserProgress progress = new UserProgress();

        progress.user = user;
        progress.drama = drama;
        progress.watchedEpisodeNumber = watchedEpisodeNumber;

        return progress;
    }

    // 기존 시청 진도 변경
    public void updateWatchedEpisodeNumber(
            Integer watchedEpisodeNumber
    ) {
        this.watchedEpisodeNumber = watchedEpisodeNumber;
    }
}

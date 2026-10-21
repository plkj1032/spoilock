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
}

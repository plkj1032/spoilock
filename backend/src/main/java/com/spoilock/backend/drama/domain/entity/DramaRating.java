package com.spoilock.backend.drama.domain.entity;

import com.spoilock.backend.common.entity.AuditedEntity;
import com.spoilock.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name ="drama_ratings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_drama_rating_user_drama",
                        columnNames = {"user_id", "drama_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DramaRating extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drama_id", nullable = false)
    private Drama drama;

    // 화면 별점의 두 배: 1 = 0.5점, 10 = 5점
    @Column(nullable = false)
    private Integer score;

    public static DramaRating create(
            User user,
            Drama drama,
            int score
    ) {
        DramaRating rating = new DramaRating();
        rating.user = user;
        rating.drama = drama;
        rating.changeScore(score);
        return rating;
    }

    public void changeScore(int score) {
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException(
                    "별점 점수는 0부터 10 사이여야 합니다."
            );
        }

        this.score = score;
    }
}

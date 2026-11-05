package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.domain.entity.DramaRating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DramaRatingRepository extends JpaRepository<DramaRating, Long> {
}

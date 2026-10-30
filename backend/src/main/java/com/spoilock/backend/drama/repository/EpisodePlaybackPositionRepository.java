package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.domain.entity.EpisodePlaybackPosition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodePlaybackPositionRepository
        extends JpaRepository<EpisodePlaybackPosition, Long> {
}

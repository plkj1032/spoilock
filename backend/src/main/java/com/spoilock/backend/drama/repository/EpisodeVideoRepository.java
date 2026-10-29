
package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.domain.entity.EpisodeVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpisodeVideoRepository
        extends JpaRepository<EpisodeVideo, Long> {

    Optional<EpisodeVideo> findByEpisode_Id(Long episodeId);

    boolean existsByEpisode_Drama_Id(Long dramaId);
}

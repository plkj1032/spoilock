package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.domain.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeRepository extends JpaRepository<Episode, Long>,
    EpisodeRepositoryCustom{

    List<Episode> findByDrama_IdOrderByEpisodeNumberAsc(
            Long dramaId
    );

    boolean existsByDrama_IdAndEpisodeNumber(
            Long dramaId,
            Integer episodeNumber
    );

}

package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

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

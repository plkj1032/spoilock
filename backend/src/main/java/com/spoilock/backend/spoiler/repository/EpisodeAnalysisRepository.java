package com.spoilock.backend.spoiler.repository;

import com.spoilock.backend.spoiler.entity.EpisodeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpisodeAnalysisRepository
    extends JpaRepository<EpisodeAnalysis, Long> {

    Optional<EpisodeAnalysis> findByEpisode_Id(Long episodeId);

    boolean existsByEpisode_Drama_Id(Long dramaId);

}

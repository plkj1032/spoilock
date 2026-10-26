package com.spoilock.backend.spoiler.repository;

import com.spoilock.backend.spoiler.entity.SpoilerEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpoilerEventRepository
    extends JpaRepository<SpoilerEvent, Long> {

    boolean existsByEpisode_Drama_Id(Long dramaId);
}

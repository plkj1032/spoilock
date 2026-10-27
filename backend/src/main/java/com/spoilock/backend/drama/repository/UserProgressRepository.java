package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProgressRepository
    extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUser_IdAndDrama_Id(
            Long userId,
            Long dramaId
    );

    List<UserProgress> findByUser_IdAndDrama_IdIn(
            Long userId,
            Collection<Long> dramaIds
    );

}

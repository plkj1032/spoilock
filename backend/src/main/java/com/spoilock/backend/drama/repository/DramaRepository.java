package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.entity.Drama;
import com.spoilock.backend.drama.enums.DramaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DramaRepository
    extends JpaRepository<Drama, Long> {

    List<Drama> findByStatusOrderByIdDesc(
            DramaStatus status
    );

    Optional<Drama> findByIdAndStatus(
            Long id,
            DramaStatus status
    );

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(
            String title,
            Long id
    );

}

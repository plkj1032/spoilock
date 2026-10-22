package com.spoilock.backend.drama.repository;

import com.spoilock.backend.drama.entity.Drama;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DramaRepository extends JpaRepository<Drama, Long> {
}
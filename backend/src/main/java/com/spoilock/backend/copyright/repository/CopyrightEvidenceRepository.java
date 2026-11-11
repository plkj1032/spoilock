package com.spoilock.backend.copyright.repository;

import com.spoilock.backend.copyright.domain.entity.CopyrightEvidence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CopyrightEvidenceRepository
        extends JpaRepository<CopyrightEvidence, Long> {

    // 특정 이용 허락 건의 증빙 목록
    @EntityGraph(attributePaths = {"uploadedBy"})
    Page<CopyrightEvidence> findByLicense_Id(
            Long licenseId,
            Pageable pageable
    );

    // 증빙 ID뿐 아니라 소속 이용 허락 건도 함께 확인
    @EntityGraph(attributePaths = {"uploadedBy"})
    Optional<CopyrightEvidence> findByIdAndLicense_Id(
            Long evidenceId,
            Long licenseId
    );
}
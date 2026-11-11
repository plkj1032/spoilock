package com.spoilock.backend.copyright.repository;

import com.spoilock.backend.copyright.domain.entity.CopyrightLicense;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface CopyrightLicenseRepository
        extends JpaRepository<CopyrightLicense, Long>,
        JpaSpecificationExecutor<CopyrightLicense> {

    // 목록 조회: 작품과 담당자 정보를 함께 조회
    @Override
    @EntityGraph(attributePaths = {
            "drama",
            "createdBy",
            "updatedBy"
    })
    Page<CopyrightLicense> findAll(
            Specification<CopyrightLicense> specification,
            Pageable pageable
    );

    // 상세 조회
    @Override
    @EntityGraph(attributePaths = {
            "drama",
            "createdBy",
            "updatedBy"
    })
    Optional<CopyrightLicense> findById(Long id);
}
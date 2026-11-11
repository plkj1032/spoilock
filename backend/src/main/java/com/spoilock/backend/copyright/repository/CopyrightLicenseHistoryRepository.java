package com.spoilock.backend.copyright.repository;

import com.spoilock.backend.copyright.domain.entity.CopyrightLicenseHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CopyrightLicenseHistoryRepository
        extends JpaRepository<CopyrightLicenseHistory, Long> {

    Page<CopyrightLicenseHistory> findByLicense_Id(
            Long licenseId,
            Pageable pageable
    );
}
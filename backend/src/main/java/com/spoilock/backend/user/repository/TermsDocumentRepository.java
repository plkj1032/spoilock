package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.TermsDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermsDocumentRepository
    extends JpaRepository<TermsDocument, Long> {

    List<TermsDocument> findAllByOrderByIdDesc();

    List<TermsDocument> findByStatusOrderByIdDesc(
            String status
    );

    List<TermsDocument> findByTermsTypeAndStatus(
            String termType,
            String status
    );

    boolean existsByTermsTypeAndVersion(
            String termsType,
            String version
    );

}

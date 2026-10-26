package com.spoilock.backend.report.repository;

import com.spoilock.backend.report.entity.ChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReportRepository
        extends JpaRepository<ChatReport, Long>, ChatReportRepositoryCustom {
}

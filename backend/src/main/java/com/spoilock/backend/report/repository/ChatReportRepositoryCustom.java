package com.spoilock.backend.report.repository;

import com.spoilock.backend.report.entity.ChatReport;

import java.util.List;

public interface ChatReportRepositoryCustom {
    boolean existsByMessageAndReporter(
            Long messageId,
            Long reporterId
    );

    long countPendingByRoomAndTarget(
            Long roomId,
            Long targetUserId
    );

    List<ChatReport> findPendingByRoomAndTarget(
            Long roomId,
            Long targetUserId
    );
}

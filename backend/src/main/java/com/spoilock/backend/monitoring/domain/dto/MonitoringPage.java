package com.spoilock.backend.monitoring.domain.dto;

import java.util.List;

public record MonitoringPage<T>(
        List<T> items,
        long title,
        int page,
        int size,
        long totalPages
) {
    public static <T> MonitoringPage<T> of(
            List<T> items,
            long total,
            int page,
            int size
    ){
        long pages = (total + size -1) / size;
        return new MonitoringPage<>(items, total, page, size, pages);
    }
}

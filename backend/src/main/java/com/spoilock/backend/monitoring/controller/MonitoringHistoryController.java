package com.spoilock.backend.monitoring.controller;

import com.spoilock.backend.monitoring.service.MonitoringHistoryService;
import com.spoilock.backend.monitoring.domain.dto.MonitoringPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/monitoring")
public class MonitoringHistoryController {

    private final MonitoringHistoryService service;

    @GetMapping("/chat-history")
    public MonitoringPage<MonitoringHistoryService.ChatRow> chats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long dramaId
    )
    {
        return service.chatHistory(page, dramaId);
    }

    @GetMapping("/job-history")
    public MonitoringPage<MonitoringHistoryService.JobRow> jobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long dramaId
    )
    {
        return service.jobHistory(page, dramaId);
    }

}

package com.spoilock.backend.admin.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/monitoring")
public class ChatMonitoringController {

    private final ChatMonitoringService service;

    // SecurityConfig의 /api/admin/** -> ROLE_ADMIN 규칙이 적용된다.
    @GetMapping("/chats")
    public ChatMonitoringResponse chats(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) Long dramaId
    ) {
        return service.getDashboard(range, dramaId);
    }
}

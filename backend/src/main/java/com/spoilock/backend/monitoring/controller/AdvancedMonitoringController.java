package com.spoilock.backend.monitoring.controller;

import com.spoilock.backend.monitoring.service.AdvancedMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/monitoring")
public class AdvancedMonitoringController {
    private final AdvancedMonitoringService service;

    @GetMapping("/advanced")
    public AdvancedMonitoringService.AdvancedResponse advanced(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) Long dramaId,
            @RequestParam(required = false) Long messageId) {
        if (!"24h".equals(range) && !"7d".equals(range)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range must be 24h or 7d");
        }
        if ((dramaId != null && dramaId <= 0) || (messageId != null && messageId <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid id");
        }
        return service.getDashboard(range, dramaId, messageId);
    }
}

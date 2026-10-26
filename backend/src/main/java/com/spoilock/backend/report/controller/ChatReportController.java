package com.spoilock.backend.report.controller;

import com.spoilock.backend.report.dto.ChatReportRequest;
import com.spoilock.backend.report.dto.ChatReportResponse;
import com.spoilock.backend.report.service.ChatReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatReportController {

    private final ChatReportService chatReportService;

    @PostMapping("/{roomId}/messages/{messageId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatReportResponse submit(
            @PathVariable("roomId") @Positive Long roomId,
            @PathVariable("messageId") @Positive Long messageId,
            @Valid @RequestBody ChatReportRequest request,
            @AuthenticationPrincipal Jwt jwt
    ){
        Long reporterId = Long.valueOf(jwt.getSubject());
        return chatReportService.submit(
                roomId,
                messageId,
                reporterId,
                request
        );
    }
}

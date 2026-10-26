package com.spoilock.backend.report.controller;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.redis.ChatRedisPublisher;
import com.spoilock.backend.report.dto.ChatReportDecisionRequest;
import com.spoilock.backend.report.service.ChatReportDecisionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class ChatReportDecisionController {

    private final ChatReportDecisionService chatReportDecisionService;
    private final ChatRedisPublisher chatRedisPublisher;

    @PostMapping("/{notificationId}/report-review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decide(
            @PathVariable("notificationId") @Positive Long notificationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChatReportDecisionRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        ChatRedisEvent event = chatReportDecisionService.decide(
                notificationId,
                userId,
                request
        );

        // 서비스 트랜잭션이 커밋된 뒤 강퇴 알림 전달
        if (event != null) {
            try {
                chatRedisPublisher.publish(event);
            } catch (RuntimeException e) {
                // DB 처리는 완료됐으므로 실패 응답으로 재시도를 유도하지 않음
                log.error(
                        "신고 처리 완료 후 강퇴 알림 전송 실패: roomId={}, messageId={}",
                        event.roomId(),
                        event.messageId(),
                        e
                );
            }
        }
    }
}
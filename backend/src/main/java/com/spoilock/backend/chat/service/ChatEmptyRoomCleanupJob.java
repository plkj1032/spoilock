package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.repository.ChatEmptyRoomQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEmptyRoomCleanupJob {

    private final ChatEmptyRoomQueryRepository queryRepository;
    private final ChatEmptyRoomService emptyRoomService;

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void cleanup() {
        for (Long roomId : queryRepository.findEmptyRoomIds()) {
            try {
                emptyRoomService.closeIfEmpty(roomId);
            } catch (RuntimeException e) {
                log.warn("빈 채팅방 자동 정리 실패: roomId={}", roomId, e);
            }
        }
    }
}
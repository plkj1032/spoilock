package com.spoilock.backend.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringRealtimeNotifier implements MessageListener {

    public static final String CHANNEL = "spoilock:monitoring";
    public static final String DESTINATION = "/topic/admin.monitoring";

    private static final String CHAT_CHANGED = "CHAT_ANALYSIS_CHANGED";
    private static final String MEDIA_CHANGED = "MEDIA_ANALYSIS_CHANGED";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public record Update(
            String type,
            Long messageId,
            Long episodeId,
            String stage
    ) {}

    // 기존 채팅 코드에서 사용하는 메서드
    public void publish(Long messageId, String stage) {
        publishAfterCommit(
                new Update(CHAT_CHANGED, messageId, null, stage)
        );
    }

    // 영상·자막 분석 변경 알림
    public void publishMedia(Long episodeId, String stage) {
        publishAfterCommit(
                new Update(MEDIA_CHANGED, null, episodeId, stage)
        );
    }

    private void publishAfterCommit(Update update) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            send(update);
                        }
                    }
            );
            return;
        }

        send(update);
    }

    private void send(Update update) {
        try {
            redisTemplate.convertAndSend(
                    CHANNEL,
                    objectMapper.writeValueAsString(update)
            );
        } catch (Exception e) {
            // 모니터링 알림 실패로 본 작업을 실패 처리하지 않는다.
            log.warn(
                    "[MONITOR_REALTIME_PUBLISH_FAILED] type={} messageId={} episodeId={}",
                    update.type(),
                    update.messageId(),
                    update.episodeId(),
                    e
            );
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            Update update = objectMapper.readValue(json, Update.class);

            if (update == null) {
                return;
            }

            boolean chatEvent =
                    CHAT_CHANGED.equals(update.type())
                            && update.messageId() != null;

            boolean mediaEvent =
                    MEDIA_CHANGED.equals(update.type())
                            && update.episodeId() != null;

            if (!chatEvent && !mediaEvent) {
                return;
            }

            messagingTemplate.convertAndSend(
                    DESTINATION,
                    update
            );
        } catch (Exception e) {
            log.warn("[MONITOR_REALTIME_RECEIVE_FAILED]", e);
        }
    }
}
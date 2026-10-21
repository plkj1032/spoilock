package com.spoilock.backend.chat.redis;

import tools.jackson.databind.ObjectMapper;
import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber
        implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(
            Message message,
            byte[] pattern
    ) {

        try {
            String json = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            ChatRedisEvent event =
                    objectMapper.readValue(
                            json,
                            ChatRedisEvent.class
                    );

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + event.roomId(),
                    event
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Redis 채팅 이벤트 처리에 실패했습니다.",
                    e
            );
        }
    }
}
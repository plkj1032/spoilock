package com.spoilock.backend.chat.redis;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic chatTopic;

    public void publish(ChatRedisEvent event) {

        try {
            String message =
                    objectMapper.writeValueAsString(event);

            redisTemplate.convertAndSend(
                    chatTopic.getTopic(),
                    message
            );

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Redis 채팅 이벤트 직렬화에 실패했습니다.",
                    e
            );
        }
    }
}
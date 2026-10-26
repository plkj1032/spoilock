package com.spoilock.backend.common.config;

import com.spoilock.backend.chat.redis.ChatPresenceNotifier;
import com.spoilock.backend.chat.redis.ChatRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    public static final String CHAT_CHANNEL =
            "spoilock:chat";

    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic(CHAT_CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatRedisSubscriber subscriber,
            ChannelTopic chatTopic,
            ChatPresenceNotifier presenceNotifier
    ) {
        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        // 기존 채팅 메시지 알림
        container.addMessageListener(
                subscriber,
                chatTopic
        );

        // 접속 상태 변경 알림
        container.addMessageListener(
                presenceNotifier,
                new ChannelTopic(ChatPresenceNotifier.CHANNEL)
        );

        return container;
    }
}
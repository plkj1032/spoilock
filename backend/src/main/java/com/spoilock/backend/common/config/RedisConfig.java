package com.spoilock.backend.common.config;

import com.spoilock.backend.chat.redis.ChatPresenceNotifier;
import com.spoilock.backend.chat.redis.ChatRedisSubscriber;
import com.spoilock.backend.chat.redis.ChatRoomListNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import com.spoilock.backend.user.session.LoginSessionNotifier;

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
            ChatPresenceNotifier presenceNotifier,
            ChatRoomListNotifier roomListNotifier,
            LoginSessionNotifier loginSessionNotifier
    ) {
        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        // 채팅 메시지
        container.addMessageListener(
                subscriber,
                chatTopic
        );

        // 접속 상태 변경
        container.addMessageListener(
                presenceNotifier,
                new ChannelTopic(ChatPresenceNotifier.CHANNEL)
        );

        // 채팅방 목록 변경
        container.addMessageListener(
                roomListNotifier,
                new ChannelTopic(ChatRoomListNotifier.CHANNEL)
        );

        // 다른 서버에서 발생한 로그인·로그아웃 변경
        container.addMessageListener(
                loginSessionNotifier,
                new ChannelTopic(LoginSessionNotifier.CHANNEL)
        );

        return container;
    }
}
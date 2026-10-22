package com.spoilock.backend.chat.redis;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            ChatRedisEvent event = objectMapper.readValue(
                    json,
                    ChatRedisEvent.class
            );

            List<Long> userIds =
                    chatRoomMemberRepository.findUserIdsByRoomId(
                            event.roomId()
                    );

            for (Long userId : userIds) {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/chat",
                        event
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Redis 채팅 이벤트 처리에 실패했습니다.",
                    e
            );
        }
    }
}
package com.spoilock.backend.chat.redis;

import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPresenceNotifier implements MessageListener {

    public static final String CHANNEL =
            "spoilock:chat:presence:changed";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void publish(Long roomId) {
        try {
            redisTemplate.convertAndSend(
                    CHANNEL,
                    roomId.toString()
            );
        } catch (RuntimeException e) {
            // 알림 전송 실패로 채팅 연결 자체를 종료하지 않는다.
            log.warn(
                    "접속 상태 변경 알림 발행 실패: roomId={}",
                    roomId,
                    e
            );
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Long roomId = Long.valueOf(
                    new String(
                            message.getBody(),
                            StandardCharsets.UTF_8
                    )
            );

            Map<String, Object> event = Map.of(
                    "eventType", "PRESENCE_CHANGED",
                    "roomId", roomId
            );

            var userIds =
                    chatRoomMemberRepository.findUserIdsByRoomId(roomId);

            for (Long userId : userIds) {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/chat",
                        event
                );
            }
        } catch (RuntimeException e) {
            log.warn("접속 상태 변경 알림 처리 실패", e);
        }
    }
}
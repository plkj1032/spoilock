package com.spoilock.backend.chat.redis;

import com.spoilock.backend.chat.domain.dto.ChatRoomCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomListNotifier implements MessageListener {

    public static final String CHANNEL =
            "spoilock:chat:rooms:changed";

    public static final String DESTINATION =
            "/topic/chat.rooms";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // 방 생성 트랜잭션이 성공적으로 끝난 뒤에만 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomCreated(ChatRoomCreatedEvent event) {
        try {
            redisTemplate.convertAndSend(CHANNEL, "ROOMS_CHANGED");
        } catch (RuntimeException e) {
            // 방은 이미 저장됐으므로 알림 실패를 방 생성 실패로 처리하지 않는다.
            log.warn(
                    "채팅방 목록 변경 알림 발행 실패: roomId={}",
                    event.roomId(),
                    e
            );
        }
    }

    // 각 서버가 Redis 알림을 받아 자신의 브라우저 연결에 전달
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            messagingTemplate.convertAndSend(
                    DESTINATION,
                    (Object) Map.of("eventType", "ROOMS_CHANGED")
            );
        } catch (RuntimeException e) {
            log.warn("채팅방 목록 변경 알림 전달 실패", e);
        }
    }
}
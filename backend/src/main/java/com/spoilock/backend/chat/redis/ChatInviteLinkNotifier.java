package com.spoilock.backend.chat.redis;

import com.spoilock.backend.chat.domain.dto.ChatInviteLinkJoinedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatInviteLinkNotifier {

    private final ChatPresenceNotifier presenceNotifier;
    private final StringRedisTemplate redisTemplate;

    // 가입 트랜잭션이 성공한 뒤에만 화면 갱신 알림 발행
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onJoined(ChatInviteLinkJoinedEvent event) {

        // 현재 방 참여자들에게 참여자 목록 갱신 알림
        presenceNotifier.publish(event.roomId());

        // 채팅방 목록을 조회하는 화면에 갱신 알림
        try {
            redisTemplate.convertAndSend(
                    ChatRoomListNotifier.CHANNEL,
                    "ROOMS_CHANGED"
            );
        } catch (RuntimeException exception) {
            // 가입은 이미 완료됐으므로 알림 실패를 가입 실패로 반환하지 않음
            log.warn(
                    "공유 링크 입장 후 채팅방 목록 갱신 알림 실패: roomId={}",
                    event.roomId(),
                    exception
            );
        }
    }
}
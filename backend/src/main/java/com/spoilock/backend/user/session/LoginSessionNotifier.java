package com.spoilock.backend.user.session;


import com.spoilock.backend.user.event.LoginSessionChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSessionNotifier implements MessageListener {
    public static final String CHANNEL = "spoilock:auth:session-changed";

    private final StringRedisTemplate redisTemplate;
    private final LoginWebSocketSessions webSocketSessions;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onSessionChanged(LoginSessionChangedEvent event){
        //현재 서버의 연결은 REDIS 장애 여부와 관계없이 정리
        webSocketSessions.closeOlderSessions(
                event.userId(),
                event.tokenVersion()
        );
        try {
            redisTemplate.convertAndSend(
                    CHANNEL,
                    event.userId() + ":" + event.tokenVersion()
            );
        } catch(RuntimeException exception){
            //로그인은 이미 완료됨. 알림 실패로 로그인 실패를 반환하지 않음
            log.warn("로그인 변경 이벤트 발행 실패: userId={}",
                    event.userId(),
                    exception);
        }
    }
    @Override
    public void onMessage(Message message, byte[] pattern){
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            String[] values = payload.split(":", -1);
            if(values.length != 2){
                log.warn("잘못된 로그인 변경 이벤트 형식");
                return;
            }
            Long userId = Long.valueOf(values[0]);
            Long tokenVersion = Long.valueOf(values[1]);

            if(userId <= 0 || tokenVersion < 0){
                log.warn("잘못된 로그인 변경 이벤트 값");
                return;
            }
            webSocketSessions.closeOlderSessions(
                    userId,tokenVersion
            );
        } catch (RuntimeException exception){
            log.warn("로그인 변경 이벤트 처리 실패", exception);
        }
    }
}

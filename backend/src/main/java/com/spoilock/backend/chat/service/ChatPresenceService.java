package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.redis.ChatPresenceNotifier;
import com.spoilock.backend.chat.redis.ChatPresenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final ChatPresenceRepository chatPresenceRepository;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ChatPresenceNotifier chatPresenceNotifier;

    // 이 서버에 연결된 WebSocket만 관리한다.
    // 전체 서버의 접속 여부는 Redis 기록을 합쳐서 조회한다.
    private final Map<String, ConnectionState> connections =
            new ConcurrentHashMap<>();

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        String sessionId = StompHeaderAccessor
                .wrap(event.getMessage())
                .getSessionId();

        if (sessionId == null) {
            return;
        }

        connections.putIfAbsent(sessionId, new ConnectionState());
    }

    public void touch(
            String sessionId,
            Long roomId,
            Long userId
    ) {
        ConnectionState state = connections.get(sessionId);

        if (state == null) {
            throw new AccessDeniedException(
                    "유효한 채팅 연결이 아닙니다."
            );
        }

        synchronized (state) {
            if (state.closed) {
                throw new AccessDeniedException(
                        "이미 종료된 채팅 연결입니다."
                );
            }

            if (state.roomId != null
                    && (!state.roomId.equals(roomId)
                    || !state.userId.equals(userId))) {
                throw new AccessDeniedException(
                        "다른 채팅방은 새 연결로 접속해 주세요."
                );
            }

            chatRoomAccessService.checkReadAccess(roomId, userId);

            boolean firstPresence = state.roomId == null;

            // Redis 저장에 성공한 뒤 연결 정보를 확정한다.
            chatPresenceRepository.touch(
                    roomId,
                    userId,
                    state.connectionId
            );

            state.roomId = roomId;
            state.userId = userId;

            // 주기적인 갱신마다 전체 회원에게 알리지 않는다.
            if (firstPresence) {
                chatPresenceNotifier.publish(roomId);
            }
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        ConnectionState state =
                connections.remove(event.getSessionId());

        if (state == null) {
            return;
        }

        synchronized (state) {
            state.closed = true;

            if (state.roomId == null) {
                return;
            }

            try {
                chatPresenceRepository.remove(
                        state.roomId,
                        state.userId,
                        state.connectionId
                );

                chatPresenceNotifier.publish(state.roomId);
            } catch (RuntimeException e) {
                log.warn(
                        "채팅 접속 기록 제거 실패: roomId={}, userId={}",
                        state.roomId,
                        state.userId,
                        e
                );
            }
        }
    }

    private static final class ConnectionState {

        // 서버가 달라도 연결 식별자가 겹치지 않도록 생성한다.
        private final String connectionId =
                UUID.randomUUID().toString();

        private Long roomId;
        private Long userId;
        private boolean closed;
    }
}
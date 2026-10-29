package com.spoilock.backend.user.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginWebSocketSessions
        implements WebSocketHandlerDecoratorFactory {

    private final JwtDecoder jwtDecoder;

    private final ConcurrentMap<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private static final CloseStatus SESSION_INVALID =
            new CloseStatus(1008, "SESSION_INVALID");

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(
                    WebSocketSession session
            ) throws Exception {
                sessions.put(session.getId(), session);

                try {
                    super.afterConnectionEstablished(session);

                    // HTTP 연결 인증 직후 다른 로그인이 발생한 경우도 확인
                    validateSession(session);
                } catch (Exception exception) {
                    sessions.remove(session.getId(), session);
                    closeSession(session);
                    throw exception;
                }
            }

            @Override
            public void afterConnectionClosed(
                    WebSocketSession session,
                    CloseStatus closeStatus
            ) throws Exception {
                sessions.remove(session.getId(), session);
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    public void closeOlderSessions(
            Long userId,
            Long currentTokenVersion
    ) {
        String subject = userId.toString();

        for (WebSocketSession session : sessions.values()) {
            if (!(session.getPrincipal()
                    instanceof JwtAuthenticationToken authentication)) {
                continue;
            }

            if (!subject.equals(authentication.getToken().getSubject())) {
                continue;
            }

            Object claim = authentication.getToken().getClaim("ver");

            // 늦게 도착한 예전 이벤트가 새 로그인을 종료하면 안 됨
            if (!(claim instanceof Number version)
                    || version.longValue() < currentTokenVersion) {
                closeSession(session);
            }
        }
    }

    // Redis 이벤트를 놓쳤거나 토큰이 만료된 연결도 정리
    @Scheduled(
            initialDelay = 10000,
            fixedDelay = 10000
    )
    public void validateOpenSessions() {
        for (WebSocketSession session : sessions.values()) {
            validateSession(session);
        }
    }

    private void validateSession(WebSocketSession session) {
        if (!session.isOpen()) {
            sessions.remove(session.getId(), session);
            return;
        }

        if (!(session.getPrincipal()
                instanceof JwtAuthenticationToken authentication)) {
            closeSession(session);
            return;
        }

        try {
            jwtDecoder.decode(
                    authentication.getToken().getTokenValue()
            );
        } catch (JwtException exception) {
            closeSession(session);
        } catch (RuntimeException exception) {
            // DB 등 일시 장애를 세션 무효화로 처리하지 않음
            log.warn(
                    "WebSocket 인증 상태 확인 실패: sessionId={}",
                    session.getId(),
                    exception
            );
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(SESSION_INVALID);
            }

            sessions.remove(session.getId(), session);
        } catch (IOException | RuntimeException exception) {
            // 다음 정리 주기에 다시 시도할 수 있도록 목록에 유지
            log.warn(
                    "이전 로그인 WebSocket 종료 실패: sessionId={}",
                    session.getId(),
                    exception
            );
        }
    }
}
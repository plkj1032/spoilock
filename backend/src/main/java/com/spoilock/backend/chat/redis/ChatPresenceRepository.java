package com.spoilock.backend.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ChatPresenceRepository {

    // 마지막 접속 확인 후 90초가 지나면 미접속으로 판단
    private static final Duration CONNECTION_TIMEOUT =
            Duration.ofSeconds(90);

    // 아무 연결도 갱신하지 않으면 Redis 키도 자동 정리
    private static final Duration ROOM_KEY_TTL =
            Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    /**
     * 방을 보고 있는 연결을 등록하거나 유효 시간을 갱신한다.
     */
    public void touch(
            Long roomId,
            Long userId,
            String connectionId
    ) {
        long expiresAt = System.currentTimeMillis()
                + CONNECTION_TIMEOUT.toMillis();

        String key = roomKey(roomId);

        redisTemplate.opsForZSet().add(
                key,
                connectionMember(userId, connectionId),
                expiresAt
        );

        redisTemplate.expire(key, ROOM_KEY_TTL);
    }

    /**
     * 종료된 연결 하나만 제거한다.
     * 같은 회원의 다른 연결은 유지한다.
     */
    public void remove(
            Long roomId,
            Long userId,
            String connectionId
    ) {
        redisTemplate.opsForZSet().remove(
                roomKey(roomId),
                connectionMember(userId, connectionId)
        );
    }

    /**
     * 유효한 연결이 하나 이상 있는 회원 ID를 반환한다.
     */
    public Set<Long> findOnlineUserIds(Long roomId) {
        String key = roomKey(roomId);
        long now = System.currentTimeMillis();

        // 만료된 연결 기록 정리
        redisTemplate.opsForZSet().removeRangeByScore(
                key,
                Double.NEGATIVE_INFINITY,
                now
        );

        Set<String> connections =
                redisTemplate.opsForZSet().rangeByScore(
                        key,
                        now + 1,
                        Double.POSITIVE_INFINITY
                );

        if (connections == null || connections.isEmpty()) {
            return Set.of();
        }

        Set<Long> userIds = new HashSet<>();

        for (String connection : connections) {
            int separator = connection.indexOf(':');

            Long userId = Long.valueOf(
                    connection.substring(0, separator)
            );

            userIds.add(userId);
        }

        return Set.copyOf(userIds);
    }

    private String roomKey(Long roomId) {
        return "spoilock:chat:presence:room:" + roomId;
    }

    private String connectionMember(
            Long userId,
            String connectionId
    ) {
        return userId + ":" + connectionId;
    }
}
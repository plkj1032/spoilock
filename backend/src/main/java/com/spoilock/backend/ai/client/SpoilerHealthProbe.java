package com.spoilock.backend.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * 분석 서버(Python) 생존 확인.
 * 재시도·상태 조회 작업이 서버가 꺼져 있는데도 요청을 쏟아내지 않도록, 작업 전에 /health 를 한 번만 확인한다.
 * 결과는 잠시 기억한다 (살아 있음: 10초, 꺼짐: 30초) → 서버 2대 × 5초 주기 조회도 꺼져 있는 동안 30초에 1번꼴.
 */
@Slf4j
@Component
public class SpoilerHealthProbe {

    static final long UP_TTL_MS = 10_000;
    static final long DOWN_TTL_MS = 30_000;

    private final RestClient restClient;

    private volatile long checkedAt;
    private volatile boolean up;

    public SpoilerHealthProbe(SpoilerApiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.min(properties.connectTimeoutMs(), 3000)))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** 분석 서버가 요청을 받을 수 있는 상태인지 (모델 로딩 중이면 false) */
    public boolean isUp() {
        long now = System.currentTimeMillis();
        if (checkedAt != 0 && now - checkedAt < (up ? UP_TTL_MS : DOWN_TTL_MS)) {
            return up;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (checkedAt != 0 && now - checkedAt < (up ? UP_TTL_MS : DOWN_TTL_MS)) {
                return up;
            }
            boolean result = probe();
            if (result != up || checkedAt == 0) {
                if (result) {
                    log.info("[spoiler] 분석 서버 연결됨 → 밀린 분석 재시도 재개");
                } else {
                    log.warn("[spoiler] 분석 서버 응답 없음 → 재시도·상태 조회를 잠시 멈춤");
                }
            }
            up = result;
            checkedAt = System.currentTimeMillis();
            return result;
        }
    }

    private boolean probe() {
        try {
            Map<?, ?> body = restClient.get().uri("/health").retrieve().body(Map.class);
            return body != null && Boolean.TRUE.equals(body.get("ready"));
        } catch (Exception e) {
            return false;
        }
    }
}

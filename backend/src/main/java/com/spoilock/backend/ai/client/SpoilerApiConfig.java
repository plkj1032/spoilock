package com.spoilock.backend.ai.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(SpoilerApiProperties.class)
public class SpoilerApiConfig {

    @Bean
    public RestClient spoilerRestClient(SpoilerApiProperties properties) {

        HttpClient httpClient = HttpClient.newBuilder()
                // uvicorn 은 HTTP/2 업그레이드(h2c)를 지원하지 않음 → 요청 본문이 유실돼 422 발생
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        // Python 서버 내부의 Gemini 제한시간(기본 8초) + 검색 시간보다 길게
        requestFactory.setReadTimeout(
                Duration.ofMillis(properties.readTimeoutMs())
        );

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Python 관리자 API 전용 (자막 분석 작업·사건 관리).
     * 업로드가 있어 판정용보다 응답 제한시간이 길고, X-Admin-Token 헤더를 자동으로 붙인다.
     */
    @Bean
    public RestClient spoilerAdminRestClient(SpoilerApiProperties properties) {
        return adminClient(properties, properties.admin().readTimeoutMs());
    }

    /** 영상 전송 전용 (전송 시간이 길어 제한시간을 따로 둠) */
    @Bean
    public RestClient spoilerUploadRestClient(SpoilerApiProperties properties) {
        return adminClient(properties, properties.admin().uploadTimeoutMs());
    }

    private static RestClient adminClient(SpoilerApiProperties properties, long timeoutMs) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory);

        String token = properties.admin().token();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("X-Admin-Token", token.trim());
        }

        return builder.build();
    }
}
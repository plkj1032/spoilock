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
}
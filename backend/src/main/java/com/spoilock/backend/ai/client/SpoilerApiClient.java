package com.spoilock.backend.ai.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Python 스포일러 판별 서버 호출.
 * 동기 HTTP 호출이므로 반드시 트랜잭션 밖, 별도 스레드에서 호출한다.
 */
@Component
public class SpoilerApiClient {

    private final RestClient restClient;
    private final SpoilerApiProperties properties;

    public SpoilerApiClient(
            @Qualifier("spoilerRestClient") RestClient restClient,
            SpoilerApiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public SpoilerJudgeResponse judge(
            Long messageId,
            Long kbDramaId,
            String content,
            List<Integer> episodes
    ) {
        String prefilterMode =
                properties.prefilterMode() == null || properties.prefilterMode().isBlank()
                        ? null
                        : properties.prefilterMode();

        SpoilerJudgeRequest request = new SpoilerJudgeRequest(
                messageId,
                content,
                kbDramaId,
                properties.kbVersion(),
                episodes,
                "vector",
                properties.topK(),
                properties.includeEvidence(),
                properties.reasonMode(),
                prefilterMode,
                false
        );

        // 연결 실패·시간 초과·4xx/5xx 는 RestClientException 으로 던져진다 → 호출 측에서 FAILED 처리
        SpoilerJudgeResponse response = restClient.post()
                .uri("/judge")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SpoilerJudgeResponse.class);

        if (response == null) {
            throw new IllegalStateException("스포일러 분석 서버 응답이 비어 있습니다.");
        }

        if (response.messageId() != null && !messageId.equals(response.messageId())) {
            throw new IllegalStateException(
                    "스포일러 분석 응답의 메시지 ID가 요청과 다릅니다. request="
                            + messageId + ", response=" + response.messageId()
            );
        }

        return response;
    }
}

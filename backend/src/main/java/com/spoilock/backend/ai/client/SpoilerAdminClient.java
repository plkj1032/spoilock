package com.spoilock.backend.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Python 관리자 API 호출 (자막 분석 작업, 사건 관리, 작품별 색인 회차).
 * 동기 HTTP 호출이므로 DB 트랜잭션 밖에서 호출한다.
 * 실패는 모두 SpoilerAdminException 으로 바꿔 던진다.
 */
@Component
public class SpoilerAdminClient {

    private static final ParameterizedTypeReference<List<KbEvent>> EVENT_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final SpoilerApiProperties properties;

    public SpoilerAdminClient(
            @Qualifier("spoilerAdminRestClient") RestClient restClient,
            SpoilerApiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    // ============================================
    // 자막 분석 작업
    // ============================================

    /** SRT 를 보내 분석 작업 생성. replace=true 면 미검수 사건을 교체 (검수·수동 사건은 보존) */
    public KbIngestJob createIngestJob(
            byte[] srt,
            long dramaId,
            long episodeId,
            int episodeNumber,
            boolean replace
    ) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

        // 파일명은 Python 쪽 확장자 검사·기록용. 한글 파일명 인코딩 문제를 피하려고 고정 이름 사용
        String filename = "episode-" + episodeId + ".srt";
        form.add("file", new ByteArrayResource(srt) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        form.add("drama_id", String.valueOf(dramaId));
        form.add("episode_id", String.valueOf(episodeId));
        form.add("episode", String.valueOf(episodeNumber));
        form.add("kb_version", properties.kbVersion());
        form.add("language", properties.admin().language());
        form.add("replace", String.valueOf(replace));

        return call(() -> restClient.post()
                .uri("/admin/subtitles/jobs")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(KbIngestJob.class));
    }

    public KbIngestJob getIngestJob(String jobId) {
        return call(() -> restClient.get()
                .uri("/admin/subtitles/jobs/{jobId}", jobId)
                .retrieve()
                .body(KbIngestJob.class));
    }

    /** 실패한 작업 재시도 (Python 이 끝난 배치는 건너뛰고 이어서 진행) */
    public KbIngestJob retryIngestJob(String jobId) {
        return call(() -> restClient.post()
                .uri("/admin/subtitles/jobs/{jobId}/retry", jobId)
                .retrieve()
                .body(KbIngestJob.class));
    }

    // ============================================
    // 사건 관리
    // ============================================

    public List<KbEvent> listEvents(long dramaId, int episodeNumber) {
        List<KbEvent> events = call(() -> restClient.get()
                .uri("/admin/events?drama_id={dramaId}&episode={episode}&kb_version={kb}",
                        dramaId, episodeNumber, properties.kbVersion())
                .retrieve()
                .body(EVENT_LIST));
        return events == null ? List.of() : events;
    }

    public KbEvent createEvent(
            long dramaId,
            long episodeId,
            int episodeNumber,
            String description,
            String eventType,
            String factStatus
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("drama_id", dramaId);
        body.put("episode_id", episodeId);
        body.put("episode", episodeNumber);
        body.put("description", description);
        if (eventType != null) {
            body.put("event_type", eventType);
        }
        if (factStatus != null) {
            body.put("fact_status", factStatus);
        }
        body.put("kb_version", properties.kbVersion());

        return call(() -> restClient.post()
                .uri("/admin/events")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(KbEvent.class));
    }

    /**
     * 보낸 항목만 수정 (snake_case 키).
     * first_reveal_episode 에 null 을 넣으면 '미지정'으로 되돌림.
     */
    public KbEvent updateEvent(String eventId, Map<String, Object> changes) {
        return call(() -> restClient.patch()
                .uri("/admin/events/{eventId}", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(changes)
                .retrieve()
                .body(KbEvent.class));
    }

    public void deleteEvent(String eventId) {
        call(() -> restClient.delete()
                .uri("/admin/events/{eventId}", eventId)
                .retrieve()
                .toBodilessEntity());
    }

    /** 판정에 쓰이는 사건이 있는 회차 번호 (반려만 남은 회차 제외) */
    public List<Integer> indexedEpisodes(long dramaId) {
        KbEpisodes result = call(() -> restClient.get()
                .uri("/kb/episodes?drama_id={dramaId}&kb_version={kb}",
                        dramaId, properties.kbVersion())
                .retrieve()
                .body(KbEpisodes.class));
        return result == null || result.episodes() == null ? List.of() : result.episodes();
    }

    // ============================================
    // 공통: 오류 변환
    // ============================================
    private <T> T call(Supplier<T> action) {
        try {
            return action.get();

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            throw new SpoilerAdminException(status, detail(e, status), e);

        } catch (ResourceAccessException e) {
            // 연결 실패·시간 초과
            throw new SpoilerAdminException(503, "분석 서버에 연결할 수 없습니다.", e);
        }
    }

    /** FastAPI 오류 본문 {"detail": ...} 에서 메시지 추출 */
    private static String detail(RestClientResponseException e, int status) {
        if (status == 401 || status == 403) {
            return "분석 서버 관리자 토큰이 맞지 않습니다. SPOILER_ADMIN_TOKEN 설정을 확인해 주세요.";
        }
        try {
            PythonError error = e.getResponseBodyAs(PythonError.class);
            if (error != null && error.detail() != null) {
                return String.valueOf(error.detail());
            }
        } catch (Exception ignored) {
            // 본문이 JSON 이 아니면 아래 기본 문구
        }
        return "분석 서버 오류 (" + status + ")";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PythonError(Object detail) {
    }
}

package com.spoilock.backend.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Map;

/**
 * Python 스포일러 판별 서버(FastAPI) 연동 설정.
 * application.properties 의 app.spoiler.* 값과 연결된다.
 */
@ConfigurationProperties("app.spoiler")
public record SpoilerApiProperties(

        // FastAPI 주소
        @DefaultValue("http://127.0.0.1:8000")
        String baseUrl,

        // 연결 제한시간 / 응답 제한시간 (ms)
        @DefaultValue("2000")
        long connectTimeoutMs,

        @DefaultValue("15000")
        long readTimeoutMs,

        // FastAPI /judge 요청 옵션
        @DefaultValue("kb1")
        String kbVersion,

        @DefaultValue("20")
        int topK,

        @DefaultValue("true")
        boolean includeEvidence,

        @DefaultValue("short")
        String reasonMode,

        // 비어 있으면 Python 서버 기본값(PREFILTER_MODE) 사용
        String prefilterMode,

        // 판정 결과 → 공개 정책
        // 검증되지 않은 사건 회차(데모 데이터)를 공개 기준으로 쓸지. 로컬 데모에서만 true
        @DefaultValue("false")
        boolean allowUnverifiedDemo,

        // 1차 필터가 '일반 대화'로 확인한 메시지 공개
        @DefaultValue("true")
        boolean publishSkippedGeneral,

        // 1차 필터가 '사건과 유사도 낮음'으로 건너뛴 메시지 공개
        @DefaultValue("false")
        boolean publishSkippedUnrelated,

        // Gemini가 '등록된 사건과 맞지 않음'으로 판정한 메시지 공개
        @DefaultValue("false")
        boolean publishNoMatch,

        // NO_MATCH 이면서 Gemini가 '드라마 이야기가 아님(about_story=false)'으로 판단한 일상 대화 공개
        @DefaultValue("true")
        boolean publishOffTopic,

        // Spring 작품 ID → Python 사건 DB 정보. 등록되지 않은 작품은 분석 보류
        Map<Long, DramaKb> dramas,

        // 데모 작품 1개 연결 (Spring 작품 ID를 .env 로 바꿀 수 있게 따로 둠)
        Demo demo,

        @DefaultValue
        Recovery recovery
) {

    public record DramaKb(
            // Python 사건 DB의 drama_id (Spring drama_id와 다를 수 있음)
            Long kbDramaId,
            // 사건이 색인된 회차 전체 (사용자 시청 회차가 아님)
            List<Integer> episodes
    ) {
    }

    public record Demo(
            Long springDramaId,
            Long kbDramaId,
            List<Integer> episodes
    ) {
    }

    /** 유실·중단된 분석 작업 복구 */
    public record Recovery(
            @DefaultValue("true")
            boolean enabled,

            @DefaultValue("30000")
            long intervalMs,

            // 이 시간 넘게 PENDING이면 다시 분석
            @DefaultValue("20")
            long pendingAfterSeconds,

            // 이 시간 넘게 PROCESSING이면 멈춘 작업으로 보고 다시 분석
            @DefaultValue("180")
            long processingAfterSeconds,

            // 생성 후 이 시간 안의 FAILED 메시지는 재시도 (Python 재시작·Gemini 일시 오류 대비)
            @DefaultValue("10")
            long retryFailedWithinMinutes,

            @DefaultValue("20")
            int batchSize
    ) {
    }

    /** Spring 작품 ID에 연결된 Python 사건 DB. 없으면 null (분석 보류) */
    public DramaKb findDrama(Long dramaId) {
        if (dramaId == null) {
            return null;
        }
        if (dramas != null && dramas.containsKey(dramaId)) {
            return dramas.get(dramaId);
        }
        if (demo != null && dramaId.equals(demo.springDramaId())) {
            return new DramaKb(demo.kbDramaId(), demo.episodes());
        }
        return null;
    }
}
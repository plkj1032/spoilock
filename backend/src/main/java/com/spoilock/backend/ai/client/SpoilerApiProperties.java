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

        // 사건이 하나도 등록되지 않은 작품(분석 서버가 확인)의 채팅·게시글은 판정할 내용이 없으므로 공개
        // false 면 예전처럼 보류(검토 대기). 분석 서버에 연결할 수 없을 때는 이 값과 무관하게 실패 → 재시도
        @DefaultValue("true")
        boolean publishUnindexedDrama,

        // Spring 작품 ID → Python 사건 DB 정보. 등록되지 않은 작품은 분석 보류
        Map<Long, DramaKb> dramas,

        // 데모 작품 1개 연결 (Spring 작품 ID를 .env 로 바꿀 수 있게 따로 둠)
        Demo demo,

        @DefaultValue
        Recovery recovery,

        // 관리자 기능(자막 분석·사건 관리) 연동
        @DefaultValue
        Admin admin
) {

    /** Python 관리자 API (/admin/subtitles/jobs, /admin/events, /kb/episodes) 연동 설정 */
    public record Admin(
            // Python .env 의 ADMIN_TOKEN 과 같은 값. X-Admin-Token 헤더로 전송 (비우면 헤더 생략)
            String token,

            // 자막 업로드·사건 수정 응답 제한시간 (판정용보다 길게)
            @DefaultValue("30000")
            long readTimeoutMs,

            // 영상(최대 100MB) 전송 제한시간. 전송 시간까지 포함되므로 길게
            @DefaultValue("600000")
            long uploadTimeoutMs,

            // 자막 언어 (현재 영어 자막만 운영)
            @DefaultValue("en")
            String language,

            // 진행 중인 자막 분석 작업 상태 조회 주기
            @DefaultValue("5000")
            long pollIntervalMs,

            // 작품별 사건 색인 회차 목록 캐시 시간
            @DefaultValue("60")
            long kbCacheSeconds
    ) {
    }

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

    /**
     * application.properties 에 직접 적은 작품 연결 (dramas[n] / demo).
     * 기본 연결은 SpoilerKbService 가 Python /kb/episodes 로 조회하며, 이 값은 수동 지정·장애 시 대체용.
     */
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
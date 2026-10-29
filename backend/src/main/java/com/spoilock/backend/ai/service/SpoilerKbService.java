package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.client.SpoilerApiProperties;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * 작품별 스포일러 판정 범위 (Spring 작품 → Python 사건 DB).
 *
 * 우선순위
 *  1) application.properties 의 app.spoiler.dramas[작품ID] (수동 지정)
 *  2) Python GET /kb/episodes 결과 (짧게 캐시). Python drama_id = Spring dramas.id
 *  3) Python 조회 실패 + 캐시 없음 → app.spoiler.demo 설정 (기존 방식)
 *
 * 판정 범위는 1화 ~ 사건이 있는 마지막 회차.
 * (검수된 사건의 최초 공개 회차가 사건이 없는 앞 회차일 수 있어, 중간 회차도 범위에 포함)
 */
@Slf4j
@Service
public class SpoilerKbService {

    private final SpoilerAdminClient adminClient;
    private final SpoilerApiProperties properties;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(List<Integer> episodes, Instant fetchedAt) {
    }

    public SpoilerKbService(
            SpoilerAdminClient adminClient,
            SpoilerApiProperties properties
    ) {
        this.adminClient = adminClient;
        this.properties = properties;
    }

    /**
     * 작품의 판정 범위 조회 결과.
     *
     * @param kb        판정에 쓸 사건 DB 정보 (판정 가능할 때만)
     * @param noEvents  분석 서버가 "이 작품에 사건이 하나도 없다"고 확인함 → 판정할 내용 없음
     *                  (kb == null 이고 noEvents == false 면 분석 서버에 물어볼 수 없었음)
     */
    public record KbLookup(SpoilerApiProperties.DramaKb kb, boolean noEvents) {

        static KbLookup of(SpoilerApiProperties.DramaKb kb) {
            return new KbLookup(kb, false);
        }

        static final KbLookup NO_EVENTS = new KbLookup(null, true);
        static final KbLookup UNKNOWN = new KbLookup(null, false);
    }

    /** 판정에 쓸 사건 DB 정보. 사건이 없거나 확인할 수 없으면 null */
    public SpoilerApiProperties.DramaKb findDrama(Long dramaId) {
        return lookup(dramaId).kb();
    }

    public KbLookup lookup(Long dramaId) {

        if (dramaId == null) {
            return KbLookup.UNKNOWN;
        }

        Map<Long, SpoilerApiProperties.DramaKb> manual = properties.dramas();
        if (manual != null && manual.containsKey(dramaId)) {
            return KbLookup.of(manual.get(dramaId));
        }

        List<Integer> indexed = indexedEpisodes(dramaId);

        if (indexed == null) {
            // 분석 서버에 물어볼 수 없음 → 기존 demo 설정으로 대체 (없으면 UNKNOWN)
            SpoilerApiProperties.DramaKb fallback = properties.findDrama(dramaId);
            return fallback != null ? KbLookup.of(fallback) : KbLookup.UNKNOWN;
        }

        if (indexed.isEmpty()) {
            return KbLookup.NO_EVENTS;
        }

        int last = Collections.max(indexed);

        return KbLookup.of(new SpoilerApiProperties.DramaKb(
                dramaId,
                IntStream.rangeClosed(1, last).boxed().toList()
        ));
    }

    /** 사건 등록·검수·분석 완료 후 호출 → 다음 판정에서 최신 회차 목록 사용 */
    public void evictAll() {
        cache.clear();
    }

    /** null = 조회 실패 (캐시도 없음) */
    private List<Integer> indexedEpisodes(Long dramaId) {

        Instant now = Instant.now();
        Cached cached = cache.get(dramaId);
        long ttl = properties.admin().kbCacheSeconds();

        if (cached != null && cached.fetchedAt().plusSeconds(ttl).isAfter(now)) {
            return cached.episodes();
        }

        try {
            List<Integer> episodes = adminClient.indexedEpisodes(dramaId);
            cache.put(dramaId, new Cached(List.copyOf(episodes), now));
            return episodes;

        } catch (Exception e) {
            log.warn("[spoiler] 사건 회차 조회 실패 dramaId={}: {}", dramaId, e.getMessage());
            // 만료된 캐시라도 있으면 사용 (분석 서버 일시 중단 대비)
            return cached != null ? cached.episodes() : null;
        }
    }
}

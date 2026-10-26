package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.ai.client.KbEvent;
import com.spoilock.backend.ai.client.SpoilerAdminClient;
import com.spoilock.backend.ai.service.SpoilerKbService;
import com.spoilock.backend.spoiler.dto.AdminSpoilerEventResponse;
import com.spoilock.backend.spoiler.dto.CreateManualEventRequest;
import com.spoilock.backend.spoiler.dto.ReviewSpoilerEventRequest;
import com.spoilock.backend.spoiler.dto.UpdateSpoilerEventRequest;
import com.spoilock.backend.spoiler.repository.EpisodeAnalysisRepository;
import com.spoilock.backend.spoiler.repository.EpisodeKbRef;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 스포일러 사건 관리 (원본은 Python 사건 DB, Spring 은 권한 확인 후 중계).
 * 사건 내용은 MySQL 에 저장하지 않는다.
 *
 * 사건이 바뀌면 판정 회차 캐시를 비워 다음 채팅 판정부터 반영한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpoilerEventAdminService {

    private final EpisodeAnalysisRepository analysisRepository;
    private final SpoilerAdminClient adminClient;
    private final SpoilerKbService kbService;

    private EpisodeKbRef findEpisode(Long episodeId) {
        return analysisRepository.findKbRef(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회차를 찾을 수 없습니다."
                ));
    }

    // ============================================
    // 1. 회차별 사건 목록
    // ============================================
    public List<AdminSpoilerEventResponse> getEvents(Long episodeId) {

        EpisodeKbRef episode = findEpisode(episodeId);

        // 회차 번호로 조회 (기존 데모 데이터는 episode_id 가 Spring 값과 다를 수 있음)
        return adminClient.listEvents(episode.dramaId(), episode.episodeNumber())
                .stream()
                .map(AdminSpoilerEventResponse::from)
                .toList();
    }

    // ============================================
    // 2. 수동 사건 등록 (검수 전 상태)
    // ============================================
    public AdminSpoilerEventResponse createManualEvent(
            Long episodeId,
            CreateManualEventRequest request
    ) {
        EpisodeKbRef episode = findEpisode(episodeId);

        KbEvent created = adminClient.createEvent(
                episode.dramaId(),
                episode.episodeId(),
                episode.episodeNumber(),
                request.eventSummary().trim(),
                blankToNull(request.eventType()),
                blankToNull(request.factStatus())
        );

        kbService.evictAll();

        log.info("스포일러 사건 수동 등록: episodeId={}, eventId={}",
                episodeId, created.eventId());

        return AdminSpoilerEventResponse.from(created);
    }

    // ============================================
    // 3. 사건 검수 (승인 / 반려 / 대기로 되돌림)
    // ============================================
    public AdminSpoilerEventResponse reviewEvent(
            String eventId,
            ReviewSpoilerEventRequest request
    ) {
        String status = request.verificationStatus();

        if ("VERIFIED".equals(status) && request.firstRevealEpisode() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "승인하려면 최초 공개 회차를 함께 지정해 주세요."
            );
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("verification_status", status);
        if (request.firstRevealEpisode() != null) {
            changes.put("first_reveal_episode", request.firstRevealEpisode());
        }

        KbEvent updated = adminClient.updateEvent(eventId, changes);

        kbService.evictAll();

        log.info("스포일러 사건 검수: eventId={}, status={}, firstReveal={}",
                eventId, status, request.firstRevealEpisode());

        return AdminSpoilerEventResponse.from(updated);
    }

    // ============================================
    // 4. 사건 내용 수정
    // ============================================
    public AdminSpoilerEventResponse updateEvent(
            String eventId,
            UpdateSpoilerEventRequest request
    ) {
        Map<String, Object> changes = new LinkedHashMap<>();

        if (request.eventSummary() != null) {
            changes.put("description", request.eventSummary().trim());
        }
        if (request.eventType() != null) {
            changes.put("event_type", request.eventType());
        }
        if (request.factStatus() != null) {
            changes.put("fact_status", request.factStatus());
        }

        if (changes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "수정할 항목이 없습니다."
            );
        }

        KbEvent updated = adminClient.updateEvent(eventId, changes);

        log.info("스포일러 사건 수정: eventId={}, fields={}", eventId, changes.keySet());

        return AdminSpoilerEventResponse.from(updated);
    }

    // ============================================
    // 5. 사건 삭제
    // ============================================
    public void deleteEvent(String eventId) {

        adminClient.deleteEvent(eventId);

        kbService.evictAll();

        log.info("스포일러 사건 삭제: eventId={}", eventId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

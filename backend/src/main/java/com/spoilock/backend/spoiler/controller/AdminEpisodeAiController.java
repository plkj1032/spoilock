package com.spoilock.backend.spoiler.controller;

import com.spoilock.backend.spoiler.dto.*;
import com.spoilock.backend.spoiler.service.AdminEpisodeAiService;
import com.spoilock.backend.spoiler.service.SpoilerEventAdminService;
import com.spoilock.backend.spoiler.service.SubtitleAnalysisService;
import com.spoilock.backend.spoiler.service.VideoAnalysisService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminEpisodeAiController {

    private final AdminEpisodeAiService service;
    private final SubtitleAnalysisService analysisService;
    private final SpoilerEventAdminService eventService;
    private final VideoAnalysisService videoAnalysisService;

    // ============================================
    // 자막·분석
    // ============================================

    @GetMapping("/episodes/{episodeId}/analysis")
    public AdminEpisodeAiResponse getAnalysis(
            @PathVariable Long episodeId
    ) {
        return service.getAnalysis(episodeId);
    }

    @PostMapping(
            value = "/episodes/{episodeId}/subtitles",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AdminEpisodeAiResponse uploadSubtitle(
            @PathVariable Long episodeId,
            @RequestParam("file") MultipartFile file
    ) {
        return service.uploadSubtitle(episodeId, file);
    }

    // 자막 교체용 삭제 (등록된 사건은 유지)
    @DeleteMapping("/episodes/{episodeId}/subtitles")
    public AdminEpisodeAiResponse deleteSubtitle(
            @PathVariable Long episodeId
    ) {
        return service.deleteSubtitle(episodeId);
    }

    // 분석 요청 버튼. replace=true: 재분석 (미검수 사건 교체, 검수·수동 사건 보존)
    @PostMapping("/episodes/{episodeId}/analysis/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    // withVideo=true: 같은 회차에 등록된 MP4 화면도 함께 분석 (자막 + 영상)
    public AdminEpisodeAiResponse requestAnalysis(
            @PathVariable Long episodeId,
            @RequestParam(defaultValue = "false") boolean replace,
            @RequestParam(defaultValue = "false") boolean withVideo
    ) {
        return analysisService.requestAnalysis(episodeId, replace, withVideo);
    }

    // 등록된 MP4 로 분석 (음성 인식 → 자막 → 사건 추출). replace=true: 재분석
    @PostMapping("/episodes/{episodeId}/analysis/video-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AdminEpisodeAiResponse requestVideoAnalysis(
            @PathVariable Long episodeId,
            @RequestParam(defaultValue = "false") boolean replace
    ) {
        return videoAnalysisService.requestAnalysis(episodeId, replace);
    }

    // ============================================
    // 스포일러 사건 (원본: Python 사건 DB)
    // ============================================

    @GetMapping("/episodes/{episodeId}/spoiler-events")
    public List<AdminSpoilerEventResponse> getEvents(
            @PathVariable Long episodeId
    ) {
        return eventService.getEvents(episodeId);
    }

    @PostMapping("/episodes/{episodeId}/spoiler-events")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSpoilerEventResponse createManualEvent(
            @PathVariable Long episodeId,
            @Valid @RequestBody CreateManualEventRequest request
    ) {
        return eventService.createManualEvent(episodeId, request);
    }

    // 승인: {"verificationStatus":"VERIFIED","firstRevealEpisode":3} / 반려: {"verificationStatus":"REJECTED"}
    @PatchMapping("/spoiler-events/{eventId}/review")
    public AdminSpoilerEventResponse review(
            @PathVariable String eventId,
            @Valid @RequestBody ReviewSpoilerEventRequest request
    ) {
        return eventService.reviewEvent(eventId, request);
    }

    @PatchMapping("/spoiler-events/{eventId}")
    public AdminSpoilerEventResponse update(
            @PathVariable String eventId,
            @Valid @RequestBody UpdateSpoilerEventRequest request
    ) {
        return eventService.updateEvent(eventId, request);
    }

    @DeleteMapping("/spoiler-events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String eventId
    ) {
        eventService.deleteEvent(eventId);
    }
}

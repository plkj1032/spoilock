
package com.spoilock.backend.spoiler.controller;

import com.spoilock.backend.spoiler.dto.*;
import com.spoilock.backend.spoiler.service.AdminEpisodeAiService;

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

    @PostMapping("/episodes/{episodeId}/analysis/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AdminEpisodeAiResponse requestAnalysis(
            @PathVariable Long episodeId
    ) {
        return service.requestAnalysis(episodeId);
    }

    @GetMapping("/episodes/{episodeId}/spoiler-events")
    public List<AdminSpoilerEventResponse> getEvents(
            @PathVariable Long episodeId
    ) {
        return service.getEvents(episodeId);
    }

    @PostMapping("/episodes/{episodeId}/spoiler-events")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSpoilerEventResponse createManualEvent(
            @PathVariable Long episodeId,
            @Valid @RequestBody CreateManualEventRequest request
    ) {
        return service.createManualEvent(episodeId, request);
    }

    @PatchMapping("/spoiler-events/{eventId}/review")
    public AdminSpoilerEventResponse review(
            @PathVariable Long eventId,
            @Valid @RequestBody ReviewSpoilerEventRequest request
    ) {
        return service.reviewEvent(eventId, request);
    }
}

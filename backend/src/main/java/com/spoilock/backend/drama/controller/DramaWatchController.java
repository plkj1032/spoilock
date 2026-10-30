package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.domain.dto.EpisodePlaybackResponse;
import com.spoilock.backend.drama.domain.dto.WatchEpisodeResponse;
import com.spoilock.backend.drama.service.DramaWatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watch/dramas")
public class DramaWatchController {

    private final DramaWatchService dramaWatchService;

    @GetMapping("/{dramaId}/episodes")
    public ResponseEntity<List<WatchEpisodeResponse>> getEpisodes(
            @PathVariable("dramaId") Long dramaId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(dramaWatchService.getEpisodes(dramaId));
    }

    @GetMapping("/{dramaId}/episodes/{episodeId}/playback")
    public ResponseEntity<EpisodePlaybackResponse> getPlayback(
            @PathVariable("dramaId") Long dramaId,
            @PathVariable("episodeId") Long episodeId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(dramaWatchService.getPlayback(dramaId, episodeId));
    }
}
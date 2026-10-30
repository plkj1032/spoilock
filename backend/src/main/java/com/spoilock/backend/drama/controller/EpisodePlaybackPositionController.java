package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.domain.dto.PlaybackPositionResponse;
import com.spoilock.backend.drama.domain.dto.SavePlaybackPositionRequest;
import com.spoilock.backend.drama.service.EpisodePlaybackPositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watch/dramas")
public class EpisodePlaybackPositionController {
    private final EpisodePlaybackPositionService positionService;

    @GetMapping("/{dramaId}/episodes/{episodeId}/position")
    public ResponseEntity<PlaybackPositionResponse> getPosition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("dramaId") Long dramaId,
            @PathVariable("episodeId") Long episodeId
            ){
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(positionService.getPosition(userId, dramaId, episodeId));
    }

    @GetMapping("/{dramaId}/resume")
    public ResponseEntity<PlaybackPositionResponse> getLatestPosition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("dramaId") Long dramaId
    ){
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(positionService.getLatestPosition(userId, dramaId));
    }

    @PutMapping("/{dramaId}/episodes/{episodeId}/position")
    public ResponseEntity<PlaybackPositionResponse> savePosition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("dramaId") Long dramaId,
            @PathVariable("episodeId") Long episodeId,
            @Valid @RequestBody SavePlaybackPositionRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(positionService.savePosition(
                        userId,
                        dramaId,
                        episodeId,
                        request
                ));
    }
}

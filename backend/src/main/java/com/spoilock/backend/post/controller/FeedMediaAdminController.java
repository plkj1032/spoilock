package com.spoilock.backend.post.controller;

import com.spoilock.backend.post.service.PostMediaService;
import com.spoilock.backend.post.service.PostMediaService.MediaInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feed/media")
@RequiredArgsConstructor
public class FeedMediaAdminController {

    private final PostMediaService service;

    public record ReviewRequest(
            boolean approved,
            @Min(0) Integer requiredEpisodeNumber
    ) {}

    @GetMapping("/pending")
    public List<MediaInfo> pending() {
        return service.pendingForAdmin();
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> file(
            @PathVariable Long mediaId
    ) {
        return FeedMediaController.respond(
                service.adminFile(mediaId)
        );
    }

    @PostMapping("/{mediaId}/review")
    public void review(
            @PathVariable Long mediaId,
            @Valid @RequestBody ReviewRequest request
    ) {
        service.review(
                mediaId,
                request.approved(),
                request.requiredEpisodeNumber()
        );
    }
}
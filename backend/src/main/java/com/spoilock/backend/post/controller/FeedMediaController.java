package com.spoilock.backend.post.controller;

import com.spoilock.backend.post.service.PostMediaService;
import com.spoilock.backend.post.service.PostMediaService.MediaFile;
import com.spoilock.backend.post.service.PostMediaService.MediaInfo;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/feed/posts")
@RequiredArgsConstructor
public class FeedMediaController {

    private final PostMediaService service;

    @PostMapping(
            value = "/{postId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public List<MediaInfo> upload(
            @PathVariable Long postId,
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.upload(
                postId,
                Long.valueOf(jwt.getSubject()),
                files
        );
    }

    @GetMapping("/{postId}/media")
    public List<MediaInfo> list(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.list(
                postId,
                Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{postId}/media/{mediaId}")
    public ResponseEntity<Resource> file(
            @PathVariable Long postId,
            @PathVariable Long mediaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        MediaFile media = service.file(
                postId,
                mediaId,
                Long.valueOf(jwt.getSubject())
        );

        return respond(media);
    }

    static ResponseEntity<Resource> respond(MediaFile media) {
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(media.contentType())
                )
                .cacheControl(CacheControl.noStore())
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(new FileSystemResource(media.path()));
    }
}
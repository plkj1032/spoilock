package com.spoilock.backend.post.controller;

import com.spoilock.backend.post.service.PostMediaService;
import com.spoilock.backend.post.service.PostMediaService.MediaFile;
import com.spoilock.backend.post.service.PostMediaService.MediaInfo;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
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

    // 원본. 스포일러 블러 대상이면 reveal=true('이미지 보기' 클릭)일 때만 내려줌
    @GetMapping("/{postId}/media/{mediaId}")
    public ResponseEntity<Resource> file(
            @PathVariable Long postId,
            @PathVariable Long mediaId,
            @RequestParam(defaultValue = "false") boolean reveal,
            @AuthenticationPrincipal Jwt jwt
    ) {
        MediaFile media = service.file(
                postId,
                mediaId,
                Long.valueOf(jwt.getSubject()),
                reveal
        );

        return respond(media);
    }

    // 블러용 아주 작은 미리보기 (원본 내용은 알아볼 수 없음)
    @GetMapping("/{postId}/media/{mediaId}/preview")
    public ResponseEntity<Resource> preview(
            @PathVariable Long postId,
            @PathVariable Long mediaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        MediaFile media = service.preview(
                postId,
                mediaId,
                Long.valueOf(jwt.getSubject())
        );

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(new ByteArrayResource(media.bytes()));
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
                .body(new ByteArrayResource(media.bytes()));
    }
}
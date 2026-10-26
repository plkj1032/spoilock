
package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.dto.AdminEpisodeVideoResponse;
import com.spoilock.backend.drama.service.AdminEpisodeVideoService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/episodes/{episodeId}/video")
@RequiredArgsConstructor
public class AdminEpisodeVideoController {

    private final AdminEpisodeVideoService service;

    // 영상 등록 정보 조회
    @GetMapping
    public AdminEpisodeVideoResponse getVideo(
            @PathVariable Long episodeId
    ) {
        return service.getVideo(episodeId);
    }

    // MP4 파일 등록
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AdminEpisodeVideoResponse upload(
            @PathVariable Long episodeId,
            @RequestParam("file") MultipartFile file
    ) {
        return service.upload(episodeId, file);
    }

    // 등록 영상 삭제
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long episodeId
    ) {
        service.delete(episodeId);
    }
}

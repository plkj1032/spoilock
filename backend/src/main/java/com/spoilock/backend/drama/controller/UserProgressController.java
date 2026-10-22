
package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.dto.ProgressResponse;
import com.spoilock.backend.drama.dto.UpdateProgressRequest;
import com.spoilock.backend.drama.service.UserProgressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/progress")
@RequiredArgsConstructor
public class UserProgressController {

    private final UserProgressService progressService;

    // 로그인 사용자 시청 진도 조회
    @GetMapping("/{dramaId}")
    public ProgressResponse getProgress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long dramaId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return progressService.getProgress(
                userId,
                dramaId
        );
    }

    // 로그인 사용자 시청 진도 저장
    @PutMapping("/{dramaId}")
    public ProgressResponse updateProgress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long dramaId,
            @Valid @RequestBody UpdateProgressRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return progressService.updateProgress(
                userId,
                dramaId,
                request.watchedEpisodeNumber()
        );
    }
}

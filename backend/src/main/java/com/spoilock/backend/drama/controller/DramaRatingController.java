package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.domain.dto.DramaRatingResponse;
import com.spoilock.backend.drama.domain.dto.SaveDramaRatingRequest;
import com.spoilock.backend.drama.service.DramaRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dramas/{dramaId}/rating")
public class DramaRatingController {
    private final DramaRatingService ratingService;

    @GetMapping
    public ResponseEntity<DramaRatingResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("dramaId") Long dramaId
    ){
        return response(
                ratingService.get(userId(jwt),dramaId)
        );
    }

    @PutMapping
    public ResponseEntity<DramaRatingResponse> save(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("dramaId") Long dramaId,
            @Valid @RequestBody SaveDramaRatingRequest request
    ){
        return response(
                ratingService.save(userId(jwt),dramaId,request)
        );
    }

    @DeleteMapping
    public ResponseEntity<DramaRatingResponse> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("dramaId") Long dramaId
    ){
        return response(
                ratingService.delete(userId(jwt), dramaId)
        );
    }

    private Long userId(Jwt jwt){
        return jwt == null
                ? null
                : Long.valueOf(jwt.getSubject());
    }
    private ResponseEntity<DramaRatingResponse> response(
            DramaRatingResponse body
    ){
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}

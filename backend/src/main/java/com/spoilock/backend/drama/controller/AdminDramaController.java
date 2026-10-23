package com.spoilock.backend.drama.controller;

import com.spoilock.backend.drama.dto.AdminDramaDetailResponse;
import com.spoilock.backend.drama.dto.AdminDramaResponse;
import com.spoilock.backend.drama.dto.CreateDramaRequest;
import com.spoilock.backend.drama.service.AdminDramaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dramas")
@RequiredArgsConstructor
public class AdminDramaController {

    private final AdminDramaService adminDramaService;

    @GetMapping
    public List<AdminDramaResponse> getAll()
    {
        return adminDramaService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDramaResponse create(
            @Valid @RequestBody CreateDramaRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long adminId = Long.valueOf(jwt.getSubject());

        return adminDramaService.create(request, adminId);
    }

    @PatchMapping("/{dramaId}/publish")
    public AdminDramaResponse publish(
            @PathVariable Long dramaId
    )
    {
        return adminDramaService.publish(dramaId);
    }


    @GetMapping("/{dramaId}")
    public AdminDramaDetailResponse getDetail(
            @PathVariable Long dramaId
    ) {
        return adminDramaService.getDetail(dramaId);
    }

    @PutMapping("/{dramaId}")
    public AdminDramaResponse update(
            @PathVariable Long dramaId,
            @Valid @RequestBody CreateDramaRequest request
    ) {
        return adminDramaService.update(dramaId, request);
    }

    @PatchMapping("/{dramaId}/hide")
    public AdminDramaResponse hide(
            @PathVariable Long dramaId
    ) {
        return adminDramaService.hide(dramaId);
    }


}

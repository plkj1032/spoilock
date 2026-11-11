package com.spoilock.backend.copyright.controller;

import com.spoilock.backend.copyright.domain.dto.CopyrightLicenseHistoryResponse;
import com.spoilock.backend.copyright.domain.dto.CreateCopyrightLicenseRequest;
import com.spoilock.backend.copyright.domain.dto.UpdateCopyrightLicenseRequest;
import com.spoilock.backend.copyright.domain.dto.CopyrightLicenseResponse;
import com.spoilock.backend.copyright.domain.type.LicenseReviewStatus;
import com.spoilock.backend.copyright.service.CopyrightLicenseService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/copyright/licenses")
@RequiredArgsConstructor
public class AdminCopyrightLicenseController {

    private final CopyrightLicenseService service;

    // 프론트에서 사용할 목록 응답 형태
    public record LicensePageResponse(
            List<CopyrightLicenseResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        public static LicensePageResponse from(
                Page<CopyrightLicenseResponse> result
        ) {
            return new LicensePageResponse(
                    result.getContent(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages(),
                    result.hasNext()
            );
        }
    }

    // =========================================
    // 1. 목록 조회
    // =========================================
    @GetMapping
    public LicensePageResponse getLicenses(
            @AuthenticationPrincipal Jwt jwt,

            @RequestParam(required = false)
            Long dramaId,

            @RequestParam(required = false)
            LicenseReviewStatus reviewStatus,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        Page<CopyrightLicenseResponse> result = service.getLicenses(
                actorId(jwt),
                dramaId,
                reviewStatus,
                keyword,
                page,
                size
        );

        return LicensePageResponse.from(result);
    }

    // =========================================
    // 2. 상세 조회
    // =========================================
    @GetMapping("/{licenseId}")
    public CopyrightLicenseResponse getLicense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long licenseId
    ) {
        return service.getLicense(
                actorId(jwt),
                licenseId
        );
    }

    // =========================================
    // 3. 등록
    // =========================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CopyrightLicenseResponse createLicense(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCopyrightLicenseRequest request
    ) {
        return service.createLicense(
                actorId(jwt),
                request
        );
    }

    // =========================================
    // 4. 수정
    // 수정 폼의 전체 값을 전달받음
    // =========================================
    @PutMapping("/{licenseId}")
    public CopyrightLicenseResponse updateLicense(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long licenseId,
            @Valid @RequestBody UpdateCopyrightLicenseRequest request
    ) {
        return service.updateLicense(
                actorId(jwt),
                licenseId,
                request
        );
    }

    // JWT의 subject에 저장된 회원 ID 사용
    private Long actorId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        try {
            Long userId = Long.valueOf(jwt.getSubject());

            if (userId <= 0) {
                throw new NumberFormatException();
            }

            return userId;
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "유효하지 않은 인증 정보입니다."
            );
        }
    }
    public record HistoryPageResponse(
            List<CopyrightLicenseHistoryResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        public static HistoryPageResponse from(
                Page<CopyrightLicenseHistoryResponse> result
        ) {
            return new HistoryPageResponse(
                    result.getContent(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages(),
                    result.hasNext()
            );
        }
    }

    @GetMapping("/{licenseId}/histories")
    public ResponseEntity<HistoryPageResponse> getHistories(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long licenseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Page<CopyrightLicenseHistoryResponse> result =
                service.getHistories(
                        actorId(jwt),
                        licenseId,
                        page,
                        size
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(HistoryPageResponse.from(result));
    }
}
package com.spoilock.backend.copyright.controller;

import com.spoilock.backend.copyright.domain.dto.CopyrightEvidenceResponse;
import com.spoilock.backend.copyright.service.CopyrightEvidenceService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/copyright/licenses/{licenseId}/evidences")
@RequiredArgsConstructor
public class AdminCopyrightEvidenceController {

    private final CopyrightEvidenceService service;

    public record EvidencePageResponse(
            List<CopyrightEvidenceResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        public static EvidencePageResponse from(
                Page<CopyrightEvidenceResponse> result
        ) {
            return new EvidencePageResponse(
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
    // 1. 증빙 목록
    // =========================================
    @GetMapping
    public ResponseEntity<EvidencePageResponse> getEvidences(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long licenseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CopyrightEvidenceResponse> result =
                service.getEvidences(
                        actorId(jwt),
                        licenseId,
                        page,
                        size
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(EvidencePageResponse.from(result));
    }

    // =========================================
    // 2. 증빙 업로드
    // multipart/form-data의 file 항목으로 받는다.
    // =========================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CopyrightEvidenceResponse> upload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long licenseId,
            @RequestPart("file") MultipartFile file
    ) {
        CopyrightEvidenceResponse result = service.upload(
                actorId(jwt),
                licenseId,
                file
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(result);
    }

    // =========================================
    // 3. 증빙 다운로드
    // 브라우저에서 직접 렌더링하지 않고 첨부 파일로 제공
    // =========================================
    @GetMapping("/{evidenceId}/download")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long licenseId,
            @PathVariable Long evidenceId
    ) {
        CopyrightEvidenceService.EvidenceDownload result =
                service.download(
                        actorId(jwt),
                        licenseId,
                        evidenceId
                );

        String disposition = ContentDisposition.attachment()
                .filename(
                        result.filename(),
                        StandardCharsets.UTF_8
                )
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(result.bytes().length)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(result.bytes());
    }

    private Long actorId(Jwt jwt) {
        if (jwt == null) {
            throw unauthorized();
        }

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw unauthorized();
        }

        try {
            long userId = Long.parseLong(subject);

            if (userId <= 0) {
                throw unauthorized();
            }

            return userId;
        } catch (NumberFormatException exception) {
            throw unauthorized();
        }
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "유효한 로그인 정보가 필요합니다."
        );
    }
}

package com.spoilock.backend.user.controller;

import com.spoilock.backend.user.service.TermsService;
import com.spoilock.backend.user.service.TermsService.TermsResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    public record CreateRequest(
            @NotBlank
            @Pattern(regexp = "TERMS|PRIVACY_NOTICE|MARKETING")
            String termsType,

            @NotBlank @Size(max = 20)
            String version,

            @NotBlank @Size(max = 200)
            String title,

            @NotBlank @Size(max = 200000)
            String content
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 200)
            String title,

            @NotBlank @Size(max = 200000)
            String content
    ) {}

    // ==========================================
    // 회원가입 화면용 공개 약관
    // ==========================================
    @GetMapping("/api/terms/current")
    public List<TermsResponse> current() {
        return termsService.current();
    }

    // ==========================================
    // 관리자용 전체 목록
    // ==========================================
    @GetMapping("/api/admin/terms")
    public List<TermsResponse> all() {
        return termsService.all();
    }

    // ==========================================
    // 관리자 약관 초안 생성
    // ==========================================
    @PostMapping("/api/admin/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public TermsResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRequest request
    ) {
        return termsService.createDraft(
                Long.valueOf(jwt.getSubject()),
                request.termsType(),
                request.version(),
                request.title(),
                request.content()
        );
    }

    // ==========================================
    // 관리자 약관 초안 수정
    // ==========================================
    @PutMapping("/api/admin/terms/{id}")
    public TermsResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequest request
    ) {
        return termsService.updateDraft(
                id,
                request.title(),
                request.content()
        );
    }

    // ==========================================
    // 관리자 약관 게시
    // ==========================================
    @PostMapping("/api/admin/terms/{id}/publish")
    public TermsResponse publish(@PathVariable Long id) {
        return termsService.publish(id);
    }
}

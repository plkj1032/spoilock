
package com.spoilock.backend.user.controller;

import com.spoilock.backend.user.service.EmailVerificationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService service;

    public record SendRequest(
            @NotBlank
            @Email
            String email
    ) {}

    public record VerifyRequest(
            @NotBlank
            @Email
            String email,

            @Pattern(regexp = "^[0-9]{6}$")
            String code
    ) {}

    // 이메일 인증번호 발송
    @PostMapping("/send")
    public ResponseEntity<Void> send(
            @Valid @RequestBody SendRequest request
    ) {
        service.sendCode(request.email());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    // 인증번호 검증 및 가입 증명 토큰 발급
    @PostMapping("/verify")
    public EmailVerificationService.VerifyResult verify(
            @Valid @RequestBody VerifyRequest request
    ) {
        return service.verifyCode(
                request.email(),
                request.code()
        );
    }
}

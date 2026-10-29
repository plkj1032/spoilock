package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.service.ChatInviteLinkService;
import com.spoilock.backend.chat.service.ChatInviteLinkService.IssuedLink;
import com.spoilock.backend.chat.service.ChatInviteLinkService.Joined;
import com.spoilock.backend.chat.service.ChatInviteLinkService.LinkInfo;
import com.spoilock.backend.chat.service.ChatInviteLinkService.Preview;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatInviteLinkController {

    private final ChatInviteLinkService inviteLinkService;

    public record IssueRequest(
            @Min(1)
            @Max(100)
            int maxUses
    ) {
    }

    public record CodeRequest(
            @NotBlank
            @Size(max = 100)
            String code
    ) {
    }

    // 방장이 현재 공유 링크 상태 조회
    @GetMapping("/rooms/{roomId}/invite-links")
    public ResponseEntity<List<LinkInfo>> current(
            @PathVariable("roomId")
            @Positive
            Long roomId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        inviteLinkService.current(
                                roomId,
                                userId(jwt)
                        )
                );
    }

    // 공유 링크 발급 또는 재발급
    @PostMapping("/rooms/{roomId}/invite-links")
    public ResponseEntity<IssuedLink> issue(
            @PathVariable("roomId")
            @Positive
            Long roomId,

            @Valid @RequestBody IssueRequest request,

            @AuthenticationPrincipal Jwt jwt
    ) {
        IssuedLink result = inviteLinkService.issue(
                roomId,
                userId(jwt),
                request.maxUses()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(result);
    }

    // 공유 링크 취소
    @DeleteMapping("/rooms/{roomId}/invite-links/{linkId}")
    public ResponseEntity<Void> revoke(
            @PathVariable("roomId")
            @Positive
            Long roomId,

            @PathVariable("linkId")
            @Positive
            Long linkId,

            @AuthenticationPrincipal Jwt jwt
    ) {
        inviteLinkService.revoke(
                roomId,
                userId(jwt),
                linkId
        );

        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    // 로그인 후 초대 내용 확인
    // 조회만 수행하며 가입하지 않음
    @PostMapping("/invite-links/preview")
    public ResponseEntity<Preview> preview(
            @Valid @RequestBody CodeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        inviteLinkService.preview(
                                request.code(),
                                userId(jwt)
                        )
                );
    }

    // 초대 수락 및 가입
    @PostMapping("/invite-links/accept")
    public ResponseEntity<Joined> accept(
            @Valid @RequestBody CodeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        inviteLinkService.accept(
                                request.code(),
                                userId(jwt)
                        )
                );
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
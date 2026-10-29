package com.spoilock.backend.user.controller;

import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.common.dto.PageResponse;
import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.dto.MyPageResponse;
import com.spoilock.backend.user.dto.UpdateProfileRequest;
import com.spoilock.backend.user.service.MyPageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/mypage")
    public ResponseEntity<MyPageResponse> getMyPage(
            @AuthenticationPrincipal Jwt jwt,

            @RequestParam(
                    name = "includeRooms",
                    defaultValue = "true"
            )
            boolean includeRooms
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(myPageService.getMyPage(
                        userId,
                        includeRooms
                ));
    }

    @GetMapping("/chat/rooms")
    public ResponseEntity<PageResponse<MyPageResponse.JoinedRoom>> getMyRooms(
            @AuthenticationPrincipal Jwt jwt,

            @RequestParam(name = "page", defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(name = "size", defaultValue = "5")
            @Min(1)
            @Max(50)
            int size,

            @RequestParam(name = "dramaId", required = false)
            @Positive
            Long dramaId,

            @RequestParam(name = "roomType", required = false)
            ChatRoomType roomType
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(myPageService.getMyRoomPage(
                        userId,
                        page,
                        size,
                        dramaId,
                        roomType
                ));
    }

    @PatchMapping("/profile")
    public ResponseEntity<MeResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(myPageService.updateProfile(userId, request));
    }
}
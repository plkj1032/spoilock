package com.spoilock.backend.chat.controller;

import com.spoilock.backend.chat.domain.dto.ChatRoomListResponse;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.service.ChatRoomQueryService;
import com.spoilock.backend.common.dto.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomQueryController {

    private final ChatRoomQueryService chatRoomQueryService;

    // 기존 전체 조회: 토론방 찾기 등에서 사용
    @GetMapping(params = "!page")
    public List<ChatRoomListResponse> getRooms() {
        return chatRoomQueryService.getActiveRooms();
    }

    // page를 전달하면 페이지 단위 조회
    @GetMapping(params = "page")
    public ResponseEntity<PageResponse<ChatRoomListResponse>> getRoomPage(
            @RequestParam(name = "page")
            @Min(0)
            int page,

            @RequestParam(name = "size", defaultValue = "10")
            @Min(1)
            @Max(50)
            int size,

            @RequestParam(name = "keyword", required = false)
            @Size(max = 200)
            String keyword,

            @RequestParam(name = "dramaId", required = false)
            @Positive
            Long dramaId,

            @RequestParam(name = "roomType", required = false)
            ChatRoomType roomType,

            @RequestParam(name = "sort", defaultValue = "members")
            @Pattern(regexp = "members|name")
            String sort
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(chatRoomQueryService.getRoomPage(
                        page,
                        size,
                        keyword,
                        dramaId,
                        roomType,
                        sort
                ));
    }

    @GetMapping("/{roomId}")
    public ChatRoomListResponse getRoom(
            @PathVariable("roomId") Long roomId
    ) {
        return chatRoomQueryService.getActiveRoom(roomId);
    }
}
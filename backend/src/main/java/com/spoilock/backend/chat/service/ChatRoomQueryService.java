package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatRoomListResponse;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.repository.ChatRoomPageQueryRepository;
import com.spoilock.backend.chat.repository.ChatRoomQueryRepository;
import com.spoilock.backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryService {

    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final ChatRoomPageQueryRepository chatRoomPageQueryRepository;
    private final ChatRoomAccessService chatRoomAccessService;

    public List<ChatRoomListResponse> getActiveRooms() {
        return chatRoomQueryRepository.findActiveRooms();
    }

    public PageResponse<ChatRoomListResponse> getRoomPage(
            int page,
            int size,
            String keyword,
            Long dramaId,
            ChatRoomType roomType,
            String sort,
            boolean includeJoinedPrivate,
            Long userId
    ) {
        return chatRoomPageQueryRepository.findPage(
                page,
                size,
                keyword,
                dramaId,
                roomType,
                sort,
                includeJoinedPrivate,
                userId
        );
    }

    public ChatRoomListResponse getActiveRoom(
            Long roomId,
            Long userId
    ) {
        ChatRoomListResponse room = chatRoomQueryRepository
                .findActiveRoomById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        if (room.roomType() == ChatRoomType.PRIVATE) {
            chatRoomAccessService.checkReadAccess(roomId, userId);
        }

        return room;
    }
}
package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatRoomListResponse;
import com.spoilock.backend.chat.repository.ChatRoomQueryRepository;
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

    public List<ChatRoomListResponse> getActiveRooms() {
        return chatRoomQueryRepository.findActiveRooms();
    }

    public ChatRoomListResponse getActiveRoom(Long roomId) {
        return chatRoomQueryRepository.findActiveRoomById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));
    }
}
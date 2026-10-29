package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatRoomClosedEvent;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.repository.ChatEmptyRoomQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatEmptyRoomService {

    private final ChatEmptyRoomQueryRepository queryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void closeIfEmpty(Long roomId) {
        var room = queryRepository.findLockedById(roomId).orElse(null);

        if (room == null
                || !room.isActive()
                || room.getRoomType() == ChatRoomType.DEFAULT) {
            return;
        }

        // 후보 조회 이후 누군가 가입했을 수 있으므로 잠금 후 재확인
        if (queryRepository.hasMembers(roomId)) {
            return;
        }

        room.deactivateEmptyRoom();

        eventPublisher.publishEvent(
                new ChatRoomClosedEvent(roomId)
        );
    }
}
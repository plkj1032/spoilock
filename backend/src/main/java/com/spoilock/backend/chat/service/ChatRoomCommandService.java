package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatRoomCreatedEvent;
import com.spoilock.backend.chat.domain.dto.ChatRoomCreateRequest;
import com.spoilock.backend.chat.domain.dto.ChatRoomCreateResponse;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.drama.entity.Drama;
import com.spoilock.backend.drama.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatRoomCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final DramaRepository dramaRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatRoomCreateResponse createRoom(
            ChatRoomCreateRequest request,
            Long userId
    ) {
        Drama drama = dramaRepository.findById(request.dramaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "작품을 찾을 수 없습니다."
                ));

        if (drama.getStatus() != DramaStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "공개된 작품을 찾을 수 없습니다."
            );
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        String roomName = request.roomName().trim();

        String description =
                request.description() == null
                        || request.description().isBlank()
                        ? null
                        : request.description().trim();

        ChatRoom room = ChatRoom.create(
                drama,
                creator,
                roomName,
                description
        );

        ChatRoom savedRoom = chatRoomRepository.save(room);

        eventPublisher.publishEvent(
                new ChatRoomCreatedEvent(savedRoom.getId())
        );

        return new ChatRoomCreateResponse(
                savedRoom.getId(),
                savedRoom.getRoomName()
        );
    }
}
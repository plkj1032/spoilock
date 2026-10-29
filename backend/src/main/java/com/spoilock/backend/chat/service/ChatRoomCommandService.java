package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatRoomCreatedEvent;
import com.spoilock.backend.chat.domain.dto.ChatRoomCreateRequest;
import com.spoilock.backend.chat.domain.dto.ChatRoomCreateResponse;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.entity.ChatRoomMember;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
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
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final DramaRepository dramaRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatRoomCreateResponse createRoom(
            ChatRoomCreateRequest request,
            Long userId
    ) {
        ChatRoomType roomType = request.roomType() == null
                ? ChatRoomType.CUSTOM
                : request.roomType();

        // 기본 토론방은 드라마 관리 기능에서만 생성
        if (roomType != ChatRoomType.CUSTOM
                && roomType != ChatRoomType.PRIVATE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "공개방 또는 비공개방만 만들 수 있습니다."
            );
        }

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

        if (creator.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "현재 채팅방을 만들 수 없는 계정입니다."
            );
        }

        String roomName = request.roomName().trim();

        String description =
                request.description() == null
                        || request.description().isBlank()
                        ? null
                        : request.description().trim();

        ChatRoom room = roomType == ChatRoomType.PRIVATE
                ? ChatRoom.createPrivate(
                drama,
                creator,
                roomName,
                description
        )
                : ChatRoom.create(
                drama,
                creator,
                roomName,
                description
        );

        ChatRoom savedRoom = chatRoomRepository.save(room);

        // 자유방·비공개방 모두 생성과 동시에 방장 가입
        chatRoomMemberRepository.save(
                ChatRoomMember.create(savedRoom, creator)
        );

        if (roomType == ChatRoomType.CUSTOM) {
            eventPublisher.publishEvent(
                    new ChatRoomCreatedEvent(savedRoom.getId())
            );
        }

        return new ChatRoomCreateResponse(
                savedRoom.getId(),
                savedRoom.getRoomName()
        );
    }
}
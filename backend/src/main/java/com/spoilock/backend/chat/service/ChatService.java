package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.entity.ChatRoomMember;
import com.spoilock.backend.chat.domain.entity.id.ChatRoomMemberId;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomAccessService chatRoomAccessService;


    @Transactional
    public ChatRedisEvent saveMessage(
            Long roomId,
            Long userId,
            String content
    ) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 채팅방입니다."
                        )
                );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 회원입니다."
                        )
                );
        chatRoomAccessService.checkNotBanned(roomId, userId);

        ChatRoomMemberId memberId =
                new ChatRoomMemberId(roomId, userId);

        if (!chatRoomMemberRepository.existsById(memberId)) {
            throw new IllegalStateException(
                    "채팅방 참여자만 메시지를 전송할 수 있습니다."
            );
        }

        ChatMessage message =
                ChatMessage.createChat(
                        room,
                        user,
                        content
                );

        ChatMessage savedMessage =
                chatMessageRepository.save(message);

        return toRedisEvent(savedMessage);
    }


    @Transactional
    public Optional<ChatRedisEvent> enterRoom(
            Long roomId,
            Long userId
    ) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 채팅방입니다."
                        )
                );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 회원입니다."
                        )
                );

        chatRoomAccessService.checkNotBanned(roomId, userId);

        ChatRoomMemberId memberId =
                new ChatRoomMemberId(
                        roomId,
                        userId
                );

        if (chatRoomMemberRepository.existsById(memberId)) {
            return Optional.empty();
        }

        ChatRoomMember member =
                ChatRoomMember.create(
                        room,
                        user
                );

        chatRoomMemberRepository.save(member);

        ChatMessage message =
                ChatMessage.createSystem(
                        room,
                        user,
                        MessageType.ENTER,
                        user.getNickname()
                                + "님이 입장했습니다."
                );

        ChatMessage savedMessage =
                chatMessageRepository.save(message);

        return Optional.of(
                toRedisEvent(savedMessage)
        );
    }


    @Transactional
    public Optional<ChatRedisEvent> leaveRoom(
            Long roomId,
            Long userId
    ) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 채팅방입니다."
                        )
                );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 회원입니다."
                        )
                );

        ChatRoomMemberId memberId =
                new ChatRoomMemberId(
                        roomId,
                        userId
                );

        Optional<ChatRoomMember> member =
                chatRoomMemberRepository.findById(memberId);

        if (member.isEmpty()) {
            return Optional.empty();
        }

        chatRoomMemberRepository.delete(
                member.get()
        );

        ChatMessage message =
                ChatMessage.createSystem(
                        room,
                        user,
                        MessageType.LEAVE,
                        user.getNickname()
                                + "님이 퇴장했습니다."
                );

        ChatMessage savedMessage =
                chatMessageRepository.save(message);

        return Optional.of(
                toRedisEvent(savedMessage)
        );
    }


    private ChatRedisEvent toRedisEvent(
            ChatMessage message
    ) {

        return new ChatRedisEvent(
                message.getRoom().getId(),
                message.getId()
        );
    }
}
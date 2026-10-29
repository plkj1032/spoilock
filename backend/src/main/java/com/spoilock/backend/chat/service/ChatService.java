package com.spoilock.backend.chat.service;

import com.spoilock.backend.ai.service.ChatAnalysisRequested;
import com.spoilock.backend.chat.domain.dto.ChatRedisEvent;
import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.entity.ChatRoomBan;
import com.spoilock.backend.chat.domain.entity.ChatRoomMember;
import com.spoilock.backend.chat.domain.entity.id.ChatRoomMemberId;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.chat.repository.ChatRoomBanRepository;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ChatRoomBanRepository chatRoomBanRepository;
    private final ApplicationEventPublisher eventPublisher;


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
        chatRoomAccessService.checkRoomAvailable(room);
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

        // 스포일러 분석 요청: 이 트랜잭션이 커밋된 뒤 별도 스레드에서 Python 서버 호출
        // (ChatAnalysisListener, @TransactionalEventListener AFTER_COMMIT)
        // 여기서 직접 호출하면 Gemini 응답을 기다리는 동안 DB 트랜잭션이 열려 있게 된다.
        eventPublisher.publishEvent(
                new ChatAnalysisRequested(
                        savedMessage.getId(),
                        room.getId(),
                        room.getDrama().getId(),
                        savedMessage.getContent()
                )
        );

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

        chatRoomAccessService.checkRoomAvailable(room);
        chatRoomAccessService.checkNotBanned(roomId, userId);

        ChatRoomMemberId memberId =
                new ChatRoomMemberId(
                        roomId,
                        userId
                );

        // 이미 가입한 회원은 기존처럼 입장 가능
        if (chatRoomMemberRepository.existsById(memberId)) {
            return Optional.empty();
        }

        // 비공개방 신규 가입은 초대 수락 기능에서만 처리
        if (room.getRoomType() == ChatRoomType.PRIVATE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "비공개 채팅방은 유효한 초대를 수락해야 입장할 수 있습니다."
            );
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

        ChatRoom room = chatRoomRepository.findOneById(roomId)
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

    @Transactional
    public ChatRedisEvent kickMember(
            Long roomId,
            Long actorId,
            Long targetUserId,
            String reason
    ){
        ChatRoom room = chatRoomRepository.findOneById(roomId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "존재하지 않는 채팅방입니다.")
                );
        User actor = userRepository.findById(actorId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "존재하지 않는 회원입니다.")
                );
        // 기본방은 관리자, 일반방/비공개방은 생성자만 가능
        chatRoomAccessService.checkKickPermission(room,actor);

        if (actorId.equals(targetUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자기 자신을 강퇴할 수 없습니다.");
        }

        if (chatRoomBanRepository.existsActiveBan(roomId, targetUserId)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 강퇴한 회원입니다.");
        }
        ChatRoomMemberId memberId =
                new ChatRoomMemberId(roomId, targetUserId);

        ChatRoomMember member = chatRoomMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방에 참여 중인 회원이 아닙니다."
                ));

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 회원입니다."
                ));

        String normalizedReason =
                reason == null || reason.isBlank()
                        ? null
                        : reason.trim();

        chatRoomBanRepository.save(
                new ChatRoomBan(room, target, actor, normalizedReason)
        );

        chatRoomMemberRepository.delete(member);

        ChatMessage message = ChatMessage.createSystem(
                room,
                target,
                MessageType.KICK,
                target.getNickname() + "님이 강퇴되었습니다."
        );

        ChatMessage savedMessage = chatMessageRepository.save(message);

        return new ChatRedisEvent(
                roomId,
                savedMessage.getId(),
                targetUserId
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
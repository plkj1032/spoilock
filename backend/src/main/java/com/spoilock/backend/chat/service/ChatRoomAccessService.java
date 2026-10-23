package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.entity.id.ChatRoomMemberId;
import com.spoilock.backend.chat.repository.ChatRoomBanRepository;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.drama.enums.DramaStatus;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomAccessService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomBanRepository chatRoomBanRepository;
    private final ChatRoomRepository chatRoomRepository;

    public void checkNotBanned(Long roomId,Long userId) {
        if(chatRoomBanRepository.existsActiveBan(roomId, userId)) {
            throw new AccessDeniedException(
                    "강퇴된 채팅방에는 접근할 수 없습니다."
            );
        }
    }

    public void checkReadAccess(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        checkRoomAvailable(room);
        checkNotBanned(roomId, userId);

        ChatRoomMemberId memberId =
                new ChatRoomMemberId(roomId, userId);

        if (!chatRoomMemberRepository.existsById(memberId)) {
            throw new AccessDeniedException(
                    "채팅방 참여자만 메시지를 조회할 수 있습니다."
            );
        }
    }

    public void checkKickPermission(ChatRoom room, User actor){
        boolean allowed = switch (room.getRoomType()){
            case DEFAULT -> actor.getRole() == UserRole.ADMIN;
            case CUSTOM,PRIVATE -> room.getCreatedBy().getId().equals(actor.getId());
        };
        if(!allowed) {
            throw new AccessDeniedException(
                    "이 채팅방에서 강퇴할 권한이 없습니다."
            );
        }
    }
    public void checkRoomAvailable(ChatRoom room) {
        if (!room.isActive()
                || room.getDrama().getStatus() != DramaStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이용 가능한 채팅방을 찾을 수 없습니다."
            );
        }
    }

}

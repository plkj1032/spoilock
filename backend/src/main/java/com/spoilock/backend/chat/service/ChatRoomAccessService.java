package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.entity.id.ChatRoomMemberId;
import com.spoilock.backend.chat.repository.ChatRoomBanRepository;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomAccessService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomBanRepository chatRoomBanRepository;

    public void checkNotBanned(Long roomId,Long userId) {
        if(chatRoomBanRepository.existsActiveBan(roomId, userId)) {
            throw new AccessDeniedException(
                    "강퇴된 채팅방에는 접근할 수 없습니다."
            );
        }
    }

    public void checkReadAccess(Long roomId, Long userId) {
        checkNotBanned(roomId, userId);

        ChatRoomMemberId memberId =
                new ChatRoomMemberId(roomId, userId);

        if (!chatRoomMemberRepository.existsById(memberId)) {
            throw new AccessDeniedException(
                    "채팅방 참여자만 메시지를 조회할 수 있습니다."
            );
        }
    }
}

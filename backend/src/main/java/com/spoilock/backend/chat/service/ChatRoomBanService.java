package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatBanResponse;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.entity.ChatRoomBan;
import com.spoilock.backend.chat.domain.type.ChatRoomBanStatus;
import com.spoilock.backend.chat.repository.ChatRoomBanRepository;
import com.spoilock.backend.chat.repository.ChatRoomRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomBanService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomBanRepository chatRoomBanRepository;
    private final UserRepository userRepository;
    private final ChatRoomAccessService chatRoomAccessService;

    public List<ChatBanResponse> getActiveBans(
            Long roomId,
            Long actorId
    ) {
        checkPermission(roomId, actorId);

        return chatRoomBanRepository.findActiveBansByRoomId(roomId);
    }

    @Transactional
    public void releaseBan(
            Long roomId,
            Long banId,
            Long actorId
    ) {
        User actor = checkPermission(roomId, actorId);

        ChatRoomBan ban = chatRoomBanRepository.findById(banId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "강퇴 이력을 찾을 수 없습니다."
                ));

        // 다른 방의 강퇴 이력을 변경하지 못하도록 확인
        if (!ban.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이 채팅방의 강퇴 이력이 아닙니다."
            );
        }

        // 이미 해제했다면 그대로 종료
        if (ban.getStatus() == ChatRoomBanStatus.RELEASED) {
            return;
        }

        ban.release(actor);
    }

    private User checkPermission(Long roomId, Long actorId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회원을 찾을 수 없습니다."
                ));

        chatRoomAccessService.checkKickPermission(room, actor);

        return actor;
    }
}
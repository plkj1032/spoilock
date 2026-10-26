package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatMemberStatusResponse;
import com.spoilock.backend.chat.redis.ChatPresenceRepository;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomMemberQueryService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ChatPresenceRepository chatPresenceRepository;

    public List<ChatMemberStatusResponse> getMembers(
            Long roomId,
            Long userId
    ) {
        chatRoomAccessService.checkReadAccess(roomId, userId);

        var members =
                chatRoomMemberRepository.findMembersByRoomId(roomId);

        Set<Long> onlineUserIds =
                chatPresenceRepository.findOnlineUserIds(roomId);

        return members.stream()
                .map(member -> new ChatMemberStatusResponse(
                        member.userId(),
                        member.nickname(),
                        onlineUserIds.contains(member.userId())
                ))
                .sorted(
                        Comparator
                                .comparing(
                                        ChatMemberStatusResponse::online
                                )
                                .reversed()
                                .thenComparing(
                                        ChatMemberStatusResponse::nickname
                                )
                                .thenComparing(
                                        ChatMemberStatusResponse::userId
                                )
                )
                .toList();
    }
}
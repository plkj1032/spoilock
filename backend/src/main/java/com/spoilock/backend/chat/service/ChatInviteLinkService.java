package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatInviteLinkJoinedEvent;
import com.spoilock.backend.chat.domain.entity.ChatInviteLink;
import com.spoilock.backend.chat.domain.entity.ChatInviteLinkUse;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.entity.ChatRoomMember;
import com.spoilock.backend.chat.domain.entity.id.ChatRoomMemberId;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.repository.ChatInviteLinkQueryRepository;
import com.spoilock.backend.chat.repository.ChatInviteLinkRepository;
import com.spoilock.backend.chat.repository.ChatInviteLinkUseRepository;
import com.spoilock.backend.chat.repository.ChatRoomMemberRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ChatInviteLinkService {

    private final ChatInviteLinkQueryRepository queryRepository;
    private final ChatInviteLinkRepository linkRepository;
    private final ChatInviteLinkUseRepository useRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ChatRoomAccessService accessService;
    private final ChatInvitationCode invitationCode;
    private final ApplicationEventPublisher eventPublisher;

    public record LinkInfo(
            Long id,
            OffsetDateTime expiresAt,
            int maxUses,
            int usedCount,
            String status
    ) {
    }

    public record IssuedLink(
            LinkInfo link,
            String code
    ) {
    }

    public record Preview(
            Long roomId,
            String roomName,
            String dramaTitle,
            String inviterNickname,
            OffsetDateTime expiresAt,
            int remainingUses,
            boolean joined
    ) {
    }

    public record Joined(
            Long roomId
    ) {
    }

    private record LockedLink(
            ChatRoom room,
            ChatInviteLink link
    ) {
    }

    // 방장이 최신 공유 링크의 상태 확인
    // 원본 코드는 저장하지 않으므로 여기서는 반환하지 않음
    public List<LinkInfo> current(
            Long roomId,
            Long actorId
    ) {
        requireOwnedPrivateRoom(roomId, actorId);

        return queryRepository.findLatest(roomId)
                .map(link -> List.of(toInfo(link)))
                .orElse(List.of());
    }

    // 공유 링크 발급
    // 재발급하면 이전 공유 링크는 취소
    public IssuedLink issue(
            Long roomId,
            Long actorId,
            int maxUses
    ) {
        requireOwnedPrivateRoom(roomId, actorId);

        if (maxUses < 1 || maxUses > 100) {
            throw fail(
                    HttpStatus.BAD_REQUEST,
                    "초대 가능 인원은 1~100명입니다."
            );
        }

        queryRepository.findLatest(roomId)
                .ifPresent(ChatInviteLink::revoke);

        String code = invitationCode.generate();

        ChatInviteLink link = linkRepository.saveAndFlush(
                ChatInviteLink.create(
                        roomId,
                        actorId,
                        invitationCode.hash(code),
                        maxUses,
                        now()
                )
        );

        return new IssuedLink(
                toInfo(link),
                code
        );
    }

    // 공유 링크 취소
    public void revoke(
            Long roomId,
            Long actorId,
            Long linkId
    ) {
        requireOwnedPrivateRoom(roomId, actorId);

        ChatInviteLink link = queryRepository.findLatest(roomId)
                .orElseThrow(this::notFound);

        // 오래된 화면의 취소 요청이 새 링크를 취소하지 않도록 확인
        if (!link.getId().equals(linkId)) {
            throw fail(
                    HttpStatus.CONFLICT,
                    "공유 링크가 변경되었습니다. 다시 확인해 주세요."
            );
        }

        link.revoke();
    }

    // 로그인 후 초대 화면에서 방 정보 확인
    // 이 단계에서는 가입하거나 사용 인원을 차감하지 않음
    public Preview preview(
            String code,
            Long userId
    ) {
        LockedLink locked = lockByCode(code);

        ChatRoom room = locked.room();
        ChatInviteLink link = locked.link();

        validateInvitation(room, link, userId);

        boolean joined = isJoined(room.getId(), userId);

        if (!joined) {
            validateNewMember(link, userId);
        }

        return new Preview(
                room.getId(),
                room.getRoomName(),
                room.getDrama().getTitle(),
                room.getCreatedBy().getNickname(),
                link.getExpiresAt().atOffset(ZoneOffset.UTC),
                link.getMaxUses() - link.getUsedCount(),
                joined
        );
    }

    // 초대 수락 후 채팅방 가입
    public Joined accept(
            String code,
            Long userId
    ) {
        LockedLink locked = lockByCode(code);

        ChatRoom room = locked.room();
        ChatInviteLink link = locked.link();

        User user = validateInvitation(room, link, userId);

        // 이미 참여 중이면 추가 가입·사용 횟수 차감 없이 이동
        if (isJoined(room.getId(), userId)) {
            return new Joined(room.getId());
        }

        validateNewMember(link, userId);

        memberRepository.saveAndFlush(
                ChatRoomMember.create(room, user)
        );

        useRepository.saveAndFlush(
                ChatInviteLinkUse.create(
                        link.getId(),
                        userId
                )
        );

        link.consume();

        eventPublisher.publishEvent(
                new ChatInviteLinkJoinedEvent(room.getId())
        );

        return new Joined(room.getId());
    }

    // 방장 여부와 현재 방 이용 가능 여부 확인
    private ChatRoom requireOwnedPrivateRoom(
            Long roomId,
            Long actorId
    ) {
        ChatRoom room = queryRepository.findLockedRoom(roomId)
                .orElseThrow(this::notFound);

        findActiveUser(actorId);

        accessService.checkRoomAvailable(room);

        if (room.getRoomType() != ChatRoomType.PRIVATE
                || !room.getCreatedBy().getId().equals(actorId)) {
            throw fail(
                    HttpStatus.FORBIDDEN,
                    "비밀 채팅방의 방장만 공유 링크를 관리할 수 있습니다."
            );
        }

        // 방장이더라도 탈퇴하거나 강퇴된 상태라면 발급 불가
        accessService.checkReadAccess(roomId, actorId);

        return room;
    }

    private LockedLink lockByCode(String code) {
        String codeHash;

        try {
            codeHash = invitationCode.hash(code);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }

        Long roomId = queryRepository
                .findRoomIdByCodeHash(codeHash)
                .orElseThrow(this::notFound);

        // 발급·취소·수락 모두 방 → 링크 순서로 잠금
        ChatRoom room = queryRepository
                .findLockedRoom(roomId)
                .orElseThrow(this::notFound);

        ChatInviteLink link = queryRepository
                .findLockedByCodeHash(codeHash)
                .orElseThrow(this::notFound);

        return new LockedLink(room, link);
    }

    private User validateInvitation(
            ChatRoom room,
            ChatInviteLink link,
            Long userId
    ) {
        User user = findActiveUser(userId);

        accessService.checkRoomAvailable(room);
        accessService.checkNotBanned(room.getId(), userId);

        if (room.getRoomType() != ChatRoomType.PRIVATE) {
            throw notFound();
        }

        // 방장이 변경되었다면 이전 방장이 발급한 링크는 사용 불가
        if (!room.getCreatedBy().getId().equals(link.getIssuerId())) {
            throw notFound();
        }

        if (link.isRevoked()
                || !link.getExpiresAt().isAfter(now())) {
            throw fail(
                    HttpStatus.GONE,
                    "만료되었거나 취소된 초대 링크입니다."
            );
        }

        findActiveUser(link.getIssuerId());

        // 링크를 발급한 방장이 현재도 방에 참여 중인지 확인
        accessService.checkReadAccess(
                room.getId(),
                link.getIssuerId()
        );

        return user;
    }

    private void validateNewMember(
            ChatInviteLink link,
            Long userId
    ) {
        // 이 링크로 입장한 뒤 탈퇴한 회원의 재사용 차단
        if (queryRepository.existsUsage(link.getId(), userId)) {
            throw fail(
                    HttpStatus.CONFLICT,
                    "이미 사용한 링크입니다. 새 초대를 받아 주세요."
            );
        }

        if (link.getUsedCount() >= link.getMaxUses()) {
            throw fail(
                    HttpStatus.GONE,
                    "초대 링크의 사용 인원이 모두 찼습니다."
            );
        }
    }

    private boolean isJoined(
            Long roomId,
            Long userId
    ) {
        return memberRepository.existsById(
                new ChatRoomMemberId(roomId, userId)
        );
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(this::notFound);

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw fail(
                    HttpStatus.FORBIDDEN,
                    "현재 이용할 수 없는 계정입니다."
            );
        }

        return user;
    }

    private LinkInfo toInfo(ChatInviteLink link) {
        String status;

        if (link.isRevoked()) {
            status = "REVOKED";
        } else if (!link.getExpiresAt().isAfter(now())) {
            status = "EXPIRED";
        } else if (link.getUsedCount() >= link.getMaxUses()) {
            status = "FULL";
        } else {
            status = "ACTIVE";
        }

        return new LinkInfo(
                link.getId(),
                link.getExpiresAt().atOffset(ZoneOffset.UTC),
                link.getMaxUses(),
                link.getUsedCount(),
                status
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private ResponseStatusException notFound() {
        return fail(
                HttpStatus.NOT_FOUND,
                "사용할 수 있는 초대 또는 채팅방을 찾을 수 없습니다."
        );
    }

    private ResponseStatusException fail(
            HttpStatus status,
            String message
    ) {
        return new ResponseStatusException(status, message);
    }
}
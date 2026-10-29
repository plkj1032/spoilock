package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatInvitationRoomResponse;
import com.spoilock.backend.chat.domain.entity.ChatRoom;
import com.spoilock.backend.chat.domain.entity.ChatRoomInvitation;
import com.spoilock.backend.chat.domain.entity.ChatRoomMember;
import com.spoilock.backend.chat.domain.entity.id.ChatRoomMemberId;
import com.spoilock.backend.chat.domain.type.ChatInvitationStatus;
import com.spoilock.backend.chat.domain.type.ChatRoomType;
import com.spoilock.backend.chat.repository.*;
import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.repository.NotificationQueryRepository;
import com.spoilock.backend.notification.repository.NotificationRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.spoilock.backend.chat.repository.ChatInviteCandidateQueryRepository;
import com.spoilock.backend.chat.repository.ChatInviteCandidateQueryRepository.CandidatePage;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ChatInvitationCode invitationCode;
    private final ChatRoomInvitationQueryRepository invitationQueryRepository;
    private final NotificationQueryRepository notificationQueryRepository;
    private final ChatInvitationRoomQueryRepository invitationRoomQueryRepository;
    private final ChatInviteCandidateQueryRepository candidateQueryRepository;

    public record InvitationCreatedResponse(
            Long invitationId,
            Long roomId,
            String roomName,
            Long receiverUserId,
            String code,
            LocalDateTime expiresAt
    ) {
    }

    @Transactional(readOnly = true)
    public List<ChatInvitationRoomResponse> getInvitableRooms(
            Long userId
    ) {
        findActiveUser(userId);

        return invitationRoomQueryRepository.findOwnedPrivateRooms(userId);
    }

    @Transactional
    public InvitationCreatedResponse sendInvitation(
            Long roomId,
            Long senderUserId,
            Long receiverUserId
    ) {
        // 같은 방의 초대 발급 요청을 순서대로 처리
        ChatRoom room = chatRoomRepository.findOneById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        if (!room.getCreatedBy().getId().equals(senderUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "방장만 초대할 수 있습니다."
            );
        }

        chatRoomAccessService.checkRoomAvailable(room);

        if (room.getRoomType() != ChatRoomType.PRIVATE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "비공개 채팅방에서만 초대코드를 발급할 수 있습니다."
            );
        }

        User sender = findActiveUser(senderUserId);

        // 방장이어도 현재 방을 탈퇴한 상태라면 초대 불가
        chatRoomAccessService.checkReadAccess(roomId, senderUserId);

        if (senderUserId.equals(receiverUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자기 자신을 초대할 수 없습니다."
            );
        }

        User receiver = findActiveUser(receiverUserId);

        chatRoomAccessService.checkNotBanned(roomId, receiverUserId);

        if (chatRoomMemberRepository.existsById(
                new ChatRoomMemberId(roomId, receiverUserId)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 채팅방에 참여 중인 회원입니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        boolean alreadyInvited = invitationQueryRepository
                .existsPendingInvitation(
                        roomId,
                        receiverUserId,
                        now
                );

        if (alreadyInvited) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 유효한 초대를 보냈습니다."
            );
        }

        String code = invitationCode.generate();

        ChatRoomInvitation invitation = invitationRepository.saveAndFlush(
                ChatRoomInvitation.create(
                        roomId,
                        senderUserId,
                        receiverUserId,
                        invitationCode.hash(code),
                        now
                )
        );

        notificationRepository.save(
                Notification.createInvitation(
                        receiver.getId(),
                        sender.getId(),
                        sender.getNickname(),
                        room.getId(),
                        room.getRoomName(),
                        invitation.getId()
                )
        );

        return new InvitationCreatedResponse(
                invitation.getId(),
                room.getId(),
                room.getRoomName(),
                receiver.getId(),
                code,
                invitation.getExpiresAt()
        );
    }

    public record InvitationAcceptedResponse(
            Long invitationId,
            Long roomId,
            String roomName
    ) {
    }

    private record LockedInvitation(
            ChatRoom room,
            ChatRoomInvitation invitation
    ) {
    }

    // 알림의 초대 ID로 수락
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InvitationAcceptedResponse acceptInvitation(
            Long invitationId,
            Long userId
    ) {
        return acceptLocked(invitationId, userId);
    }

    // 직접 입력한 코드로 수락
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InvitationAcceptedResponse acceptByCode(
            String code,
            Long userId
    ) {
        String codeHash;

        try {
            codeHash = invitationCode.hash(code);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "초대코드 형식을 확인해 주세요.",
                    e
            );
        }

        Long invitationId = invitationQueryRepository
                .findInvitationIdByCodeHash(codeHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용할 수 있는 초대를 찾을 수 없습니다."
                ));

        return acceptLocked(invitationId, userId);
    }

    private InvitationAcceptedResponse acceptLocked(
            Long invitationId,
            Long userId
    ) {
        LockedInvitation locked = lockInvitation(invitationId, userId);

        ChatRoom room = locked.room();
        ChatRoomInvitation invitation = locked.invitation();

        User receiver = findActiveUser(userId);

        chatRoomAccessService.checkRoomAvailable(room);
        chatRoomAccessService.checkNotBanned(room.getId(), userId);

        if (room.getRoomType() != ChatRoomType.PRIVATE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "비공개 채팅방 초대가 아닙니다."
            );
        }

        ChatRoomMemberId memberId =
                new ChatRoomMemberId(room.getId(), userId);

        boolean joined = chatRoomMemberRepository.existsById(memberId);

        // 수락 완료 후 재클릭: 현재도 참여 중이면 방으로 이동 가능
        if (invitation.getStatus() == ChatInvitationStatus.ACCEPTED) {
            if (!joined) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 사용한 초대입니다. 새 초대를 받아 주세요."
                );
            }

            markInvitationRead(invitation.getId(), userId);

            return acceptedResponse(room, invitation);
        }

        LocalDateTime now = LocalDateTime.now();

        requirePendingInvitation(invitation, now);

        // 수락 시점에도 초대한 사람이 유효한 방장인지 확인
        if (!room.getCreatedBy().getId()
                .equals(invitation.getSenderUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "더 이상 사용할 수 없는 초대입니다."
            );
        }

        findActiveUser(invitation.getSenderUserId());

        chatRoomAccessService.checkReadAccess(
                room.getId(),
                invitation.getSenderUserId()
        );

        if (!joined) {
            chatRoomMemberRepository.saveAndFlush(
                    ChatRoomMember.create(room, receiver)
            );
        }

        invitation.accept(now);
        markInvitationRead(invitation.getId(), userId);

        return acceptedResponse(room, invitation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void rejectInvitation(
            Long invitationId,
            Long userId
    ) {
        LockedInvitation locked = lockInvitation(invitationId, userId);
        ChatRoomInvitation invitation = locked.invitation();

        findActiveUser(userId);

        // 거절 버튼 중복 클릭은 정상 처리
        if (invitation.getStatus() == ChatInvitationStatus.REJECTED) {
            markInvitationRead(invitation.getId(), userId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        requirePendingInvitation(invitation, now);

        invitation.reject(now);
        markInvitationRead(invitation.getId(), userId);
    }

    private LockedInvitation lockInvitation(
            Long invitationId,
            Long userId
    ) {
        Long roomId = invitationQueryRepository
                .findRoomIdByInvitationId(invitationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "초대를 찾을 수 없습니다."
                ));

        // 발급·수락 모두 방 → 초대 순서로 잠금
        ChatRoom room = chatRoomRepository.findOneById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        ChatRoomInvitation invitation = invitationQueryRepository
                .findLockedById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "초대를 찾을 수 없습니다."
                ));

        // 다른 계정에는 초대 내용이나 방 정보를 반환하지 않음
        if (!invitation.getReceiverUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "사용할 수 있는 초대를 찾을 수 없습니다."
            );
        }

        return new LockedInvitation(room, invitation);
    }

    private void requirePendingInvitation(
            ChatRoomInvitation invitation,
            LocalDateTime now
    ) {
        if (invitation.getStatus() != ChatInvitationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 처리되었거나 취소된 초대입니다."
            );
        }

        if (!invitation.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "초대가 만료되었습니다. 새 초대를 받아 주세요."
            );
        }
    }

    private void markInvitationRead(
            Long invitationId,
            Long userId
    ) {
        notificationQueryRepository.markInvitationAsRead(
                invitationId,
                userId
        );
    }
    private InvitationAcceptedResponse acceptedResponse(
            ChatRoom room,
            ChatRoomInvitation invitation
    ) {
        return new InvitationAcceptedResponse(
                invitation.getId(),
                room.getId(),
                room.getRoomName()
        );
    }
    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "현재 초대를 보내거나 받을 수 없는 계정입니다."
            );
        }

        return user;
    }
    @Transactional(readOnly = true)
    public CandidatePage getCandidates(
            Long roomId,
            Long senderId,
            int page
    ) {
        findActiveUser(senderId);

        chatRoomAccessService.checkReadAccess(roomId, senderId);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        if (room.getRoomType() != ChatRoomType.PRIVATE
                || !room.getCreatedBy().getId().equals(senderId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "비공개방의 방장만 초대 목록을 조회할 수 있습니다."
            );
        }

        return candidateQueryRepository.findPage(roomId, senderId, page);
    }
}
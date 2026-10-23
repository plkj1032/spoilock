package com.spoilock.backend.chat.service;

import com.spoilock.backend.chat.domain.dto.ChatMessageHistoryResponse;
import com.spoilock.backend.chat.domain.dto.ChatMessageResponse;
import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.type.ChatBlockReason;
import com.spoilock.backend.chat.domain.type.ChatVisibility;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.chat.repository.ChatProgressQueryRepository;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageQueryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ChatProgressQueryRepository chatProgressQueryRepository;

    private static final int HISTORY_SIZE = 30;

    public ChatMessageResponse getMessage(
            Long roomId,
            Long messageId,
            Long userId
    ) {
        // 요청한 방에 대한 접근 권한부터 검사
        chatRoomAccessService.checkReadAccess(roomId, userId);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "메시지를 찾을 수 없습니다."
                ));

        // 다른 방의 메시지 ID를 넣는 우회 방지
        if (!message.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "메시지를 찾을 수 없습니다."
            );
        }

        ChatBlockReason blockReason;

        if (message.getMessageType() == MessageType.CHAT) {
            Long dramaId = message.getRoom().getDrama().getId();
            blockReason = chatBlockReason(
                    message,
                    userId,
                    () -> chatProgressQueryRepository.findWatchedEpisodeNumber(userId, dramaId)
            );
        } else {
            blockReason = systemBlockReason(message);
        }

        return toResponse(message, blockReason);
    }

    public ChatMessageHistoryResponse getHistory(
            Long roomId,
            Long cursor,
            Long userId
    ) {
        // 단건 조회와 동일하게 방 접근 권한 검사
        chatRoomAccessService.checkReadAccess(roomId, userId);

        // 실제 반환 개수보다 1개 더 조회해서
        // 더 이전 메시지가 존재하는지 확인
        List<ChatMessage> fetchedMessages =
                chatMessageRepository.findHistory(
                        roomId,
                        cursor,
                        HISTORY_SIZE + 1
                );

        boolean hasNext = fetchedMessages.size() > HISTORY_SIZE;

        // 실제 응답에는 최대 30개만 포함
        List<ChatMessage> pageMessages = new ArrayList<>(
                fetchedMessages.subList(
                        0,
                        Math.min(fetchedMessages.size(), HISTORY_SIZE)
                )
        );

        // 다음 요청에서는 현재 페이지의 가장 오래된 messageId보다
        // 이전 메시지를 조회
        Long nextCursor =
                hasNext && !pageMessages.isEmpty()
                        ? pageMessages.get(pageMessages.size() - 1).getId()
                        : null;

        /*
         * 같은 채팅방의 메시지는 같은 작품에 속하므로
         * 수신자의 작품 시청 진도를 한 번만 조회한다.
         *
         * 메시지마다 user_progress를 조회하지 않아 N+1을 방지한다.
         */
        Optional<Integer> watchedEpisodeNumber =
                pageMessages.isEmpty()
                        ? Optional.empty()
                        : chatProgressQueryRepository.findWatchedEpisodeNumber(
                        userId,
                        pageMessages.get(0)
                                .getRoom()
                                .getDrama()
                                .getId()
                );

        List<ChatMessageResponse> responses =
                pageMessages.stream()
                        .map(message -> {

                            ChatBlockReason blockReason;

                            if (message.getMessageType() == MessageType.CHAT) {
                                blockReason = chatBlockReason(
                                        message,
                                        userId,
                                        () -> watchedEpisodeNumber
                                );
                            } else {
                                blockReason = systemBlockReason(message);
                            }

                            return toResponse(message, blockReason);
                        })
                        .toList();

        /*
         * DB에서는 최신 → 과거 순으로 조회했지만
         * 채팅 화면에는 과거 → 최신 순으로 반환
         */
        List<ChatMessageResponse> orderedResponses =
                new ArrayList<>(responses);

        Collections.reverse(orderedResponses);

        return new ChatMessageHistoryResponse(
                orderedResponses,
                nextCursor,
                hasNext
        );
    }

    private ChatBlockReason systemBlockReason(ChatMessage message) {
        boolean systemMessage =
                message.getMessageType() == MessageType.ENTER
                        || message.getMessageType() == MessageType.LEAVE
                        || message.getMessageType() == MessageType.KICK;

        boolean hidden =
                message.getModerationStatus() == ModerationStatus.HIDDEN
                        || message.getModerationStatus() == ModerationStatus.REMOVED;

        boolean show = systemMessage
                && message.getAnalysisStatus() == AnalysisStatus.SKIPPED
                && !hidden;

        return show ? null : ChatBlockReason.HIDDEN;
    }

    /**
     * 일반 채팅 공개 여부. null 이면 원문 공개, 값이 있으면 그 이유로 차단.
     *
     * @param watchedEpisodeNumber 수신자의 시청 회차 (필요할 때만 조회)
     */
    private ChatBlockReason chatBlockReason(
            ChatMessage message,
            Long viewerId,
            Supplier<Optional<Integer>> watchedEpisodeNumber
    ) {
        ModerationStatus moderation = message.getModerationStatus();

        // 관리자 숨김·삭제는 누구에게도 공개하지 않음
        if (moderation == ModerationStatus.HIDDEN
                || moderation == ModerationStatus.REMOVED) {
            return ChatBlockReason.HIDDEN;
        }

        // 보낸 사람 본인에게는 원문 표시 (본인이 쓴 내용이므로 스포일러가 아님)
        if (message.getUser().getId().equals(viewerId)) {
            return null;
        }

        // 분석 대기·진행 중·실패 상태에서는 원문 비공개
        if (message.getAnalysisStatus() == AnalysisStatus.PENDING
                || message.getAnalysisStatus() == AnalysisStatus.PROCESSING) {
            return ChatBlockReason.ANALYZING;
        }

        if (message.getAnalysisStatus() == AnalysisStatus.FAILED) {
            return ChatBlockReason.FAILED;
        }

        if (message.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
            return ChatBlockReason.REVIEW;
        }

        // 서비스 공개 상태가 확정된 메시지만 허용 (NO_MATCH·UNCERTAIN 등은 PENDING 으로 보류)
        if (moderation != ModerationStatus.VISIBLE) {
            return ChatBlockReason.REVIEW;
        }

        Integer requiredEpisode = message.getRequiredEpisodeNumber();

        // 분석 완료 + 공개 상태에서 필요 회차가 없으면 스포 아님
        if (requiredEpisode == null) {
            return null;
        }

        // 비정상 회차는 공개하지 않음
        if (requiredEpisode <= 0) {
            return ChatBlockReason.REVIEW;
        }

        Optional<Integer> watched = watchedEpisodeNumber.get();

        return watched.isPresent() && watched.get() >= requiredEpisode
                ? null
                : ChatBlockReason.NOT_WATCHED;
    }

    private ChatMessageResponse toResponse(
            ChatMessage message,
            ChatBlockReason blockReason
    ) {
        boolean show = blockReason == null;

        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getUser().getId(),
                message.getUser().getNickname(),
                message.getMessageType(),
                show ? ChatVisibility.SHOW : ChatVisibility.BLOCK,
                show ? message.getContent() : null,
                message.getCreatedAt(),
                blockReason
        );
    }
}
package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.SpoilerJudgeRequest;
import com.spoilock.backend.chat.domain.type.MessageType;
import com.spoilock.backend.chat.repository.ChatMessageRepository;
import com.spoilock.backend.common.enums.ModerationStatus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 스포 판정에 함께 보낼 직전 대화.
 *
 * 한 줄만 보면 스포가 아닌 메시지를 잡기 위함
 *  - 질문 → 답: "누가 설치함?" → "민재현ㅋ"
 *  - 나눠 보낸 문장: "서윤하가" / "영상을" / "남겼었어"
 * 판정 대상은 여전히 지금 메시지 1건. 앞 메시지의 공개 여부는 바꾸지 않는다.
 *
 * 조회에 실패해도 분석은 계속한다 (맥락 없이 메시지만 판정).
 */
@Slf4j
@Service
public class ChatContextService {

    private static final int MAX_TEXT_LENGTH = 300;

    private final ChatMessageRepository chatMessageRepository;
    private final boolean enabled;
    private final int size;
    private final long windowSeconds;

    public ChatContextService(
            ChatMessageRepository chatMessageRepository,
            @Value("${app.spoiler.context.enabled:true}") boolean enabled,
            @Value("${app.spoiler.context.size:6}") int size,
            @Value("${app.spoiler.context.window-seconds:180}") long windowSeconds
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.enabled = enabled;
        this.size = Math.min(Math.max(size, 0), 10);   // Python 은 최대 10개까지 받음
        this.windowSeconds = windowSeconds;
    }

    /**
     * @return 오래된 것 → 최근 순. 끄거나 실패하면 빈 목록
     */
    @Transactional(readOnly = true)
    public List<SpoilerJudgeRequest.ContextLine> forMessage(Long roomId, Long messageId) {
        if (!enabled || size == 0 || roomId == null || messageId == null) {
            return List.of();
        }

        try {
            ChatContextRow anchor = chatMessageRepository.findContextAnchor(messageId).orElse(null);
            if (anchor == null || anchor.createdAt() == null) {
                return List.of();
            }

            List<ChatContextRow> rows = chatMessageRepository.findContextBefore(
                    roomId,
                    messageId,
                    MessageType.CHAT,
                    ModerationStatus.REMOVED,
                    anchor.createdAt().minusSeconds(windowSeconds),
                    PageRequest.of(0, size)
            );

            List<SpoilerJudgeRequest.ContextLine> lines = new ArrayList<>();
            for (ChatContextRow row : rows) {
                if (row.content() == null || row.content().isBlank()) {
                    continue;
                }
                String text = row.content().strip();
                if (text.length() > MAX_TEXT_LENGTH) {
                    text = text.substring(0, MAX_TEXT_LENGTH);
                }
                long secondsBefore = row.createdAt() == null ? 0
                        : Math.max(0, Duration.between(row.createdAt(), anchor.createdAt()).getSeconds());

                lines.add(new SpoilerJudgeRequest.ContextLine(
                        text,
                        Objects.equals(row.userId(), anchor.userId()),
                        secondsBefore
                ));
            }

            Collections.reverse(lines);   // 최근 것부터 조회했으므로 시간순으로 뒤집음
            return lines;

        } catch (Exception e) {
            log.warn("[spoiler] 직전 대화 조회 실패 messageId={}: {}", messageId, e.toString());
            return List.of();
        }
    }
}
package com.spoilock.backend.monitoring.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.chat.domain.entity.QChatMessage;
import com.spoilock.backend.chat.domain.entity.QChatRoom;
import com.spoilock.backend.drama.domain.entity.QDrama;
import com.spoilock.backend.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class MonitoringMessageDetailService {
    private final JPAQueryFactory queryFactory;

    public record Detail(
            Long messageId,
            Long senderId,
            String senderNickname,
            Long roomId,
            String roomName,
            String roomType,
            Long dramaId,
            String dramaTitle,
            String content,
            Integer authorWatchedEpisode,
            Integer requiredEpisode,
            boolean requiredUntilFinale,
            String analysisStatus,
            String moderationStatus,
            String outcome,
            String policyCode,
            String policyDetail,
            Long durationMs,
            LocalDateTime createdAt,
            LocalDateTime analyzedAt
    ){}

    public Detail find(Long messageId){
        if (messageId == null) return null;

        QChatMessage message = QChatMessage.chatMessage;
        QChatRoom room = QChatRoom.chatRoom;
        QDrama drama = QDrama.drama;
        QUser sender = QUser.user;

        ChatMessage found = queryFactory
                .selectFrom(message)
                .join(message.room, room).fetchJoin()
                .join(room.drama,drama).fetchJoin()
                .join(message.user, sender).fetchJoin()
                .where(message.id.eq(messageId))
                .fetchOne();

        if (found == null) return null;
        return new Detail(
                found.getId(),
                found.getUser().getId(),
                found.getUser().getNickname(),
                found.getRoom().getId(),
                found.getRoom().getRoomName(),
                found.getRoom().getRoomType().name(),
                found.getRoom().getDrama().getId(),
                found.getRoom().getDrama().getTitle(),
                found.getContent(),
                found.getAuthorWatchedEpisodeNumber(),
                found.getRequiredEpisodeNumber(),
                found.isRequiredUntilFinale(),
                found.getAnalysisStatus().name(),
                found.getModerationStatus().name(),
                found.getAnalysisOutcome(),
                found.getAnalysisPolicyCode(),
                found.getAnalysisPolicyDetail(),
                found.getAnalysisDurationMs(),
                found.getCreatedAt(),
                found.getAnalyzedAt()
        );
    }
}

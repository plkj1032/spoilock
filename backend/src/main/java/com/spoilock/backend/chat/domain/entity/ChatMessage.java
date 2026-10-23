package com.spoilock.backend.chat.domain.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.chat.domain.type.MessageType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "room_id",
            nullable = false
    )
    private ChatRoom room;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "message_type",
            nullable = false,
            length = 20
    )
    private MessageType messageType = MessageType.CHAT;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Column(name = "required_episode_number")
    private Integer requiredEpisodeNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "analysis_status",
            nullable = false,
            length = 20
    )
    private AnalysisStatus analysisStatus =
            AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "moderation_status",
            nullable = false,
            length = 20
    )
    private ModerationStatus moderationStatus =
            ModerationStatus.PENDING;

    public static ChatMessage createChat(
            ChatRoom room,
            User user,
            String content){
        ChatMessage message = new ChatMessage();

        message.room = room;
        message.user = user;
        message.content = content;
        message.messageType = MessageType.CHAT;
        message.analysisStatus = AnalysisStatus.PENDING;

        return message;
    }

    public static ChatMessage createSystem(
            ChatRoom room,
            User user,
            MessageType messageType,
            String content
    ){
        if (messageType == null || messageType == MessageType.CHAT){
            throw new IllegalArgumentException("시스템 메시지는 ENTER, LEAVE, KICK만 가능합니다.");
        }
        ChatMessage message = new ChatMessage();
        message.room = room;
        message.user = user;
        message.messageType = messageType;
        message.content = content;
        message.analysisStatus = AnalysisStatus.SKIPPED;

        return message;
    }

    /**
     * 스포일러 분석 완료.
     *
     * @param publish                 true  → VISIBLE (requiredEpisodeNumber 이상 시청자에게 공개, null 이면 전원)
     *                                false → PENDING (관리자 검토 대기, 원문 차단)
     * @param requiredEpisodeNumber   공개에 필요한 회차 (publish=false 면 무시)
     */
    public void completeAnalysis(
            boolean publish,
            Integer requiredEpisodeNumber
    ) {
        this.analysisStatus = AnalysisStatus.COMPLETED;
        this.requiredEpisodeNumber = publish ? requiredEpisodeNumber : null;

        // 관리자가 이미 숨김/삭제한 메시지는 분석 결과로 다시 공개하지 않음
        if (isModeratedByAdmin()) {
            return;
        }

        this.moderationStatus = publish
                ? ModerationStatus.VISIBLE
                : ModerationStatus.PENDING;
    }

    /** 스포일러 분석 실패: 원문 차단 유지, 복구 작업이 재시도 */
    public void failAnalysis() {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.requiredEpisodeNumber = null;

        if (!isModeratedByAdmin()) {
            this.moderationStatus = ModerationStatus.PENDING;
        }
    }

    private boolean isModeratedByAdmin() {
        return moderationStatus == ModerationStatus.HIDDEN
                || moderationStatus == ModerationStatus.REMOVED;
    }
}

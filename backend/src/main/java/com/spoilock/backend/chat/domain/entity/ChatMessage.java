package com.spoilock.backend.chat.domain.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.chat.domain.type.MessageType;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    // 메시지를 작성한 시점의 진도.
    // 기존 메시지와 시스템 메시지는 null일 수 있음.
    @Column(name = "author_watched_episode_number")
    private Integer authorWatchedEpisodeNumber;

    // 마지막 회차 기준 공개 여부
    @Column(name = "required_until_finale", nullable = false)
    private boolean requiredUntilFinale = false;

    // 분석 실패 재시도 (분석 서버가 살아 있을 때 실패한 횟수만 셈)
    @Column(name = "analysis_attempts", nullable = false)
    private int analysisAttempts = 0;

    // 마지막 분석 시작 시각 (멈춘 PROCESSING 판단 기준)
    @Column(name = "analysis_started_at")
    private LocalDateTime analysisStartedAt;

    // 이 시각 이후에 다시 시도 (실패할 때마다 간격이 2배로 늘어남)
    @Column(name = "analysis_retry_at")
    private LocalDateTime analysisRetryAt;

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

    // ================================
    // AI 모니터링 V2 + V3
    // ================================

    // AI 판정 결과
    @Column(name = "analysis_outcome", length = 30)
    private String analysisOutcome;

    // AI 판정 소요 시간 (ms)
    @Column(name = "analysis_duration_ms")
    private Long analysisDurationMs;

    // AI 분석 완료 시간
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "analysis_policy_code", length = 40)
    private String analysisPolicyCode;

    @Column(name = "analysis_policy_detail", length = 255)
    private String analysisPolicyDetail;

    @Column(name = "analysis_raw_verdict", length = 30)
    private String analysisRawVerdict;

    @Column(name = "ai_response_json", columnDefinition = "LONGTEXT")
    private String aiResponseJson;


    public void recordAnalysisMetrics(
            String outcome,
            long durationMs,
            String policyCode,
            String policyDetail,
            String rawVerdict
    ) {
        this.analysisOutcome = outcome;
        this.analysisDurationMs = Math.max(0L, durationMs);
        this.analysisPolicyCode = policyCode;
        this.analysisPolicyDetail = policyDetail;
        this.analysisRawVerdict = rawVerdict;
        this.analyzedAt = LocalDateTime.now();
    }

    public void recordAiResponse(String aiResponseJson) {
        this.aiResponseJson = aiResponseJson;
    }


    public static ChatMessage createChat(
            ChatRoom room,
            User user,
            String content,
            int authorWatchedEpisodeNumber
    ) {
        if (authorWatchedEpisodeNumber < 0) {
            throw new IllegalArgumentException(
                    "작성자 시청 진도는 0 이상이어야 합니다."
            );
        }

        ChatMessage message = new ChatMessage();

        message.room = room;
        message.user = user;
        message.content = content;
        message.authorWatchedEpisodeNumber = authorWatchedEpisodeNumber;
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
    // 기존 2개 인자 호출과 호환
    public void completeAnalysis(
            boolean publish,
            Integer requiredEpisodeNumber
    ) {
        completeAnalysis(
                publish,
                requiredEpisodeNumber,
                false
        );
    }
    /**
     * AI 분석 완료 및 공개 정책 저장
     */
    public void completeAnalysis(
            boolean publish,
            Integer requiredEpisodeNumber,
            boolean untilFinale
    ) {
        this.analysisStatus = AnalysisStatus.COMPLETED;
        this.analysisAttempts = 0;
        this.analysisRetryAt = null;

        this.requiredEpisodeNumber =
                publish ? requiredEpisodeNumber : null;

        this.requiredUntilFinale =
                publish && untilFinale;

        // 관리자에 의해 숨김 또는 삭제된 메시지는 다시 공개하지 않음
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
        this.requiredUntilFinale = false;

        if (!isModeratedByAdmin()) {
            this.moderationStatus = ModerationStatus.PENDING;
        }
    }

    /** 분석 실패 + 다음 재시도 시각 예약 (baseDelaySeconds × 2^(실패횟수-1), 최대 1시간) */
    public void failAnalysis(long baseDelaySeconds) {
        failAnalysis();
        this.analysisAttempts++;
        this.analysisRetryAt = LocalDateTime.now().plusSeconds(
                com.spoilock.backend.ai.service.AnalysisRetry.delaySeconds(analysisAttempts, baseDelaySeconds));
    }

    private boolean isModeratedByAdmin() {
        return moderationStatus == ModerationStatus.HIDDEN
                || moderationStatus == ModerationStatus.REMOVED;
    }
}

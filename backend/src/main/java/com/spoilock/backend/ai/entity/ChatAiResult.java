package com.spoilock.backend.ai.entity;

import com.spoilock.backend.ai.enums.RiskLevel;
import com.spoilock.backend.chat.entity.ChatMessage;
import com.spoilock.backend.spoiler.entity.SpoilerEvent;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_ai_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAiResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 분석 대상 채팅 메시지
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "message_id",
            nullable = false,
            unique = true
    )
    private ChatMessage message;

    // 일치하는 스포일러 사건
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_event_id")
    private SpoilerEvent matchedEvent;

    // AI가 예측한 스포일러 회차
    @Column(name = "predicted_episode_number")
    private Integer predictedEpisodeNumber;

    // AI 판정 신뢰도
    @Column(
            name = "confidence_score",
            precision = 5,
            scale = 4
    )
    private BigDecimal confidenceScore;

    // 스포일러 위험도
    @Enumerated(EnumType.STRING)
    @Column(
            name = "risk_level",
            nullable = false,
            length = 20
    )
    private RiskLevel riskLevel = RiskLevel.UNKNOWN;

    // AI 판단 근거
    @Column(columnDefinition = "TEXT")
    private String reason;

    // 분석 완료 시간
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
}

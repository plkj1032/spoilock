package com.spoilock.backend.admin.monitoring;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_analysis_trace")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAnalysisTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "stage", nullable = false, length = 32)
    private String stage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public ChatAnalysisTrace(Long messageId, String stage, Long durationMs) {
        this.messageId = messageId;
        this.stage = stage;
        this.durationMs = durationMs;
        this.recordedAt = LocalDateTime.now();
    }
}

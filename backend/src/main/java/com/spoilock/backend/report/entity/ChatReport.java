package com.spoilock.backend.report.entity;

import com.spoilock.backend.chat.domain.entity.ChatMessage;
import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.report.enums.ReportReason;
import com.spoilock.backend.report.enums.ReportStatus;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_report_user",
                        columnNames = {"message_id", "reporter_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReport extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고된 채팅 메시지
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "message_id",
            nullable = false
    )
    private ChatMessage message;

    // 신고한 사용자
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reporter_id",
            nullable = false
    )
    private User reporter;

    // 신고 사유
    @Enumerated(EnumType.STRING)
    @Column(
            name = "reason",
            nullable = false,
            length = 20
    )
    private ReportReason reason;

    // 관리자 확안용 상세 설명
    @Column(name = "detail", length = 500)
    private String detail;

    // 신고 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ReportStatus status = ReportStatus.PENDING;

    // 신고 처리 완료 시간
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static ChatReport create(
            ChatMessage message,
            User reporter,
            ReportReason reason,
            String detail
    ){
        if (message == null || reporter == null || reason == null){
            throw new IllegalArgumentException(
                    "신고 메시지, 신고자, 신고 사유는 필수 입니다."
            );
        }
        if (detail != null && detail.length() > 500){
            throw new IllegalArgumentException(
                    "상세 설명은 500자 이하로 입력해 주세요"
            );
        }
        ChatReport report =  new ChatReport();

        report.message = message;
        report.reporter = reporter;
        report.reason = reason;
        report.detail = detail == null || detail.isBlank() ? null : detail.strip();
        report.status = ReportStatus.PENDING;

        return report;
    }
}

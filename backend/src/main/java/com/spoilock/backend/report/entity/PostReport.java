package com.spoilock.backend.report.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.report.enums.ReportReason;
import com.spoilock.backend.report.enums.ReportStatus;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_report_user",
                        columnNames = {"post_id", "reporter_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고된 게시글
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "post_id",
            nullable = false
    )
    private Post post;

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
}

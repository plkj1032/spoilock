package com.spoilock.backend.admin.entity;

import com.spoilock.backend.admin.enums.AdminAction;
import com.spoilock.backend.admin.enums.AdminTargetType;
import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관리자
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "admin_user_id",
            nullable = false
    )
    private User adminUser;

    // 관리자가 수행한 행동
    @Enumerated(EnumType.STRING)
    @Column(
            name = "action",
            nullable = false,
            length = 50
    )
    private AdminAction action;

    // 작업 대상 유형
    @Enumerated(EnumType.STRING)
    @Column(
            name = "target_type",
            nullable = false,
            length = 30
    )
    private AdminTargetType targetType;

    // 작업 대상 ID
    @Column(
            name = "target_id",
            nullable = false
    )
    private Long targetId;
}

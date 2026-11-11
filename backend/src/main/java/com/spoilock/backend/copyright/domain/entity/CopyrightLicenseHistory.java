package com.spoilock.backend.copyright.domain.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.user.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "copyright_license_histories")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CopyrightLicenseHistory extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "license_id",
            nullable = false,
            updatable = false
    )
    private CopyrightLicense license;

    @Column(
            name = "action",
            nullable = false,
            updatable = false,
            length = 20
    )
    private String action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "actor_id",
            nullable = false,
            updatable = false
    )
    private User actor;

    @Column(
            name = "actor_nickname",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String actorNickname;

    @Column(
            name = "before_snapshot",
            updatable = false,
            columnDefinition = "LONGTEXT"
    )
    private String beforeSnapshot;

    @Column(
            name = "after_snapshot",
            nullable = false,
            updatable = false,
            columnDefinition = "LONGTEXT"
    )
    private String afterSnapshot;

    public static CopyrightLicenseHistory create(
            CopyrightLicense license,
            User actor,
            String beforeSnapshot,
            String afterSnapshot
    ) {
        if (license == null || actor == null) {
            throw new IllegalArgumentException(
                    "이용 권한과 변경 관리자 정보가 필요합니다."
            );
        }

        if (afterSnapshot == null || afterSnapshot.isBlank()) {
            throw new IllegalArgumentException(
                    "변경 후 내용이 필요합니다."
            );
        }

        if (beforeSnapshot != null && beforeSnapshot.isBlank()) {
            throw new IllegalArgumentException(
                    "변경 전 내용은 빈 문자열일 수 없습니다."
            );
        }

        CopyrightLicenseHistory history =
                new CopyrightLicenseHistory();

        history.license = license;
        history.actor = actor;
        history.actorNickname = actor.getNickname() == null
                ? "이름 없음"
                : actor.getNickname();

        history.action = beforeSnapshot == null
                ? "CREATED"
                : "UPDATED";

        history.beforeSnapshot = beforeSnapshot;
        history.afterSnapshot = afterSnapshot;

        return history;
    }
}
package com.spoilock.backend.copyright.service;

import com.spoilock.backend.copyright.domain.dto.CopyrightLicenseResponse;
import com.spoilock.backend.copyright.domain.entity.CopyrightLicense;
import com.spoilock.backend.copyright.domain.entity.CopyrightLicenseHistory;
import com.spoilock.backend.copyright.repository.CopyrightLicenseHistoryRepository;
import com.spoilock.backend.user.entity.User;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CopyrightLicenseHistoryWriter {

    private final CopyrightLicenseHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    // 엔티티 자체가 아닌 응답 DTO를 JSON으로 보관
    // 지연 로딩 프록시나 양방향 관계가 직렬화되는 것을 방지
    public String snapshot(CopyrightLicense license) {
        return objectMapper.writeValueAsString(
                CopyrightLicenseResponse.from(license)
        );
    }

    public void recordCreated(
            CopyrightLicense license,
            User actor
    ) {
        CopyrightLicenseHistory history =
                CopyrightLicenseHistory.create(
                        license,
                        actor,
                        null,
                        snapshot(license)
                );

        historyRepository.saveAndFlush(history);
    }

    public void recordUpdated(
            CopyrightLicense license,
            User actor,
            String beforeSnapshot
    ) {
        if (beforeSnapshot == null || beforeSnapshot.isBlank()) {
            throw new IllegalArgumentException(
                    "수정 이력에는 변경 전 내용이 필요합니다."
            );
        }

        CopyrightLicenseHistory history =
                CopyrightLicenseHistory.create(
                        license,
                        actor,
                        beforeSnapshot,
                        snapshot(license)
                );

        historyRepository.saveAndFlush(history);
    }
}
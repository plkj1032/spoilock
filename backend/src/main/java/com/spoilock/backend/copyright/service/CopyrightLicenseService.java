package com.spoilock.backend.copyright.service;

import com.spoilock.backend.copyright.domain.dto.CopyrightLicenseHistoryResponse;
import com.spoilock.backend.copyright.domain.dto.CreateCopyrightLicenseRequest;
import com.spoilock.backend.copyright.domain.dto.UpdateCopyrightLicenseRequest;
import com.spoilock.backend.copyright.domain.dto.CopyrightLicenseResponse;
import com.spoilock.backend.copyright.domain.entity.CopyrightLicense;
import com.spoilock.backend.copyright.domain.type.LicenseReviewStatus;
import com.spoilock.backend.copyright.repository.CopyrightLicenseHistoryRepository;
import com.spoilock.backend.copyright.repository.CopyrightLicenseRepository;

import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaRepository;

import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.enums.UserRole;
import com.spoilock.backend.user.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CopyrightLicenseService {

    private final CopyrightLicenseRepository licenseRepository;
    private final DramaRepository dramaRepository;
    private final UserRepository userRepository;
    private final Validator validator;
    private final CopyrightLicenseHistoryWriter historyWriter;
    private final CopyrightLicenseHistoryRepository historyRepository;

    // =========================================
    // 1. 목록 조회
    // 작품·확인 상태·검색어 필터 + 페이지 조회
    // =========================================
    public Page<CopyrightLicenseResponse> getLicenses(
            Long actorId,
            Long dramaId,
            LicenseReviewStatus reviewStatus,
            String keyword,
            int page,
            int size
    ) {
        requireAdmin(actorId);

        if (page < 0 || size < 1 || size > 100) {
            throw badRequest(
                    "페이지는 0 이상, 조회 개수는 1~100이어야 합니다."
            );
        }

        if (dramaId != null && dramaId <= 0) {
            throw badRequest("올바른 작품 ID가 필요합니다.");
        }

        String search = keyword == null ? "" : keyword.trim();

        if (search.length() > 200) {
            throw badRequest("검색어는 200자 이하여야 합니다.");
        }

        Specification<CopyrightLicense> specification =
                (root, query, cb) -> {
                    List<Predicate> conditions = new ArrayList<>();

                    if (dramaId != null) {
                        conditions.add(
                                cb.equal(
                                        root.get("drama").get("id"),
                                        dramaId
                                )
                        );
                    }

                    if (reviewStatus != null) {
                        conditions.add(
                                cb.equal(
                                        root.get("reviewStatus"),
                                        reviewStatus
                                )
                        );
                    }

                    if (!search.isEmpty()) {
                        String pattern =
                                "%" + escapeLike(search) + "%";

                        conditions.add(
                                cb.or(
                                        cb.like(
                                                root.<String>get("licenseName"),
                                                pattern,
                                                '!'
                                        ),
                                        cb.like(
                                                root.<String>get("rightsHolder"),
                                                pattern,
                                                '!'
                                        ),
                                        cb.like(
                                                root.get("drama")
                                                        .<String>get("title"),
                                                pattern,
                                                '!'
                                        )
                                )
                        );
                    }

                    return cb.and(
                            conditions.toArray(new Predicate[0])
                    );
                };

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return licenseRepository
                .findAll(specification, pageable)
                .map(CopyrightLicenseResponse::from);
    }

    // =========================================
    // 2. 상세 조회
    // =========================================
    public CopyrightLicenseResponse getLicense(
            Long actorId,
            Long licenseId
    ) {
        requireAdmin(actorId);

        CopyrightLicense license = findLicense(licenseId);

        return CopyrightLicenseResponse.from(license);
    }

    // =========================================
    // 3. 등록
    // =========================================
    @Transactional
    public CopyrightLicenseResponse createLicense(
            Long actorId,
            CreateCopyrightLicenseRequest request
    ) {
        User actor = requireAdmin(actorId);
        validateRequest(request);

        Drama drama = dramaRepository
                .findById(request.dramaId())
                .filter(item -> item.getStatus() != DramaStatus.DELETED)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "등록할 작품을 찾을 수 없습니다."
                        )
                );

        CopyrightLicense license;

        try {
            license = CopyrightLicense.create(
                    drama,
                    actor,

                    request.licenseName(),
                    request.rightsHolder(),
                    request.contactName(),
                    request.contactEmail(),

                    request.reviewStatus(),
                    request.videoPermission(),
                    request.subtitlePermission(),
                    request.aiPermission(),

                    request.startsOn(),
                    request.endsOn(),

                    request.scopeNotes(),
                    request.adminNotes()
            );
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }

        CopyrightLicense saved =
                licenseRepository.saveAndFlush(license);

        // 최초 등록 내용 기록
        historyWriter.recordCreated(saved, actor);

        return CopyrightLicenseResponse.from(saved);
    }

    // =========================================
    // 4. 수정
    // =========================================
    @Transactional
    public CopyrightLicenseResponse updateLicense(
            Long actorId,
            Long licenseId,
            UpdateCopyrightLicenseRequest request
    ) {
        User actor = requireAdmin(actorId);
        validateRequest(request);

        CopyrightLicense license = findLicense(licenseId);

        if (license.getVersion() != request.version().longValue()) {
            throw versionConflict();
        }

        // 반드시 license.update() 실행 전에 변경 전 내용을 저장
        String beforeSnapshot = historyWriter.snapshot(license);

        try {
            license.update(
                    actor,

                    request.licenseName(),
                    request.rightsHolder(),
                    request.contactName(),
                    request.contactEmail(),

                    request.reviewStatus(),
                    request.videoPermission(),
                    request.subtitlePermission(),
                    request.aiPermission(),

                    request.startsOn(),
                    request.endsOn(),

                    request.scopeNotes(),
                    request.adminNotes()
            );
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }

        try {
            licenseRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw versionConflict();
        }

        // 버전 및 수정 시각이 DB에 반영된 뒤 변경 후 내용 기록
        historyWriter.recordUpdated(
                license,
                actor,
                beforeSnapshot
        );

        return CopyrightLicenseResponse.from(license);
    }

    // =========================================
    // 5. 로그인 사용자 및 관리자 권한 확인
    // =========================================
    private User requireAdmin(Long actorId) {
        if (actorId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "사용자 정보를 확인할 수 없습니다."
                        )
                );

        if (actor.getAccountStatus() != AccountStatus.ACTIVE
                || actor.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "관리자만 이용할 수 있습니다."
            );
        }

        return actor;
    }

    // =========================================
    // 6. 이용 권한 기록 조회
    // =========================================
    private CopyrightLicense findLicense(Long licenseId) {
        if (licenseId == null || licenseId <= 0) {
            throw badRequest("올바른 이용 권한 ID가 필요합니다.");
        }

        return licenseRepository.findById(licenseId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "이용 권한 기록을 찾을 수 없습니다."
                        )
                );
    }

    // =========================================
    // 7. 요청 DTO 검증
    // 서비스가 다른 코드에서 호출돼도 동일하게 검사
    // =========================================
    private <T> void validateRequest(T request) {
        if (request == null) {
            throw badRequest("요청 내용이 필요합니다.");
        }

        var violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation -> violation.getMessage())
                    .sorted()
                    .findFirst()
                    .orElse("입력값을 확인해 주세요.");

            throw badRequest(message);
        }
    }

    // 검색어의 %, _를 와일드카드가 아닌 실제 문자로 처리
    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private static ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private static ResponseStatusException versionConflict() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "다른 관리자가 먼저 수정했습니다. 최신 내용을 다시 조회한 후 수정해 주세요."
        );
    }

    // 이용 권한 변경 이력 조회
    public Page<CopyrightLicenseHistoryResponse> getHistories(
            Long actorId,
            Long licenseId,
            int page,
            int size
    ) {
        requireAdmin(actorId);
        findLicense(licenseId);

        if (page < 0 || size < 1 || size > 50) {
            throw badRequest(
                    "페이지는 0 이상, 조회 개수는 1~50이어야 합니다."
            );
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return historyRepository
                .findByLicense_Id(licenseId, pageable)
                .map(CopyrightLicenseHistoryResponse::from);
    }
}
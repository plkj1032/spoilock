
package com.spoilock.backend.user.service;

import com.spoilock.backend.user.entity.TermsDocument;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.entity.UserTermsAcceptance;
import com.spoilock.backend.user.repository.TermsDocumentRepository;
import com.spoilock.backend.user.repository.UserRepository;
import com.spoilock.backend.user.repository.UserTermsAcceptanceRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsDocumentRepository termsRepository;
    private final UserTermsAcceptanceRepository acceptanceRepository;
    private final UserRepository userRepository;

    public record TermsResponse(
            Long id,
            String termsType,
            String version,
            String title,
            String content,
            boolean required,
            String status,
            LocalDateTime effectiveAt,
            LocalDateTime publishedAt
    ) {}

    private TermsResponse toResponse(TermsDocument document) {
        return new TermsResponse(
                document.getId(),
                document.getTermsType(),
                document.getVersion(),
                document.getTitle(),
                document.getContent(),
                document.isRequired(),
                document.getStatus(),
                document.getEffectiveAt(),
                document.getPublishedAt()
        );
    }

    // ==========================================
    // 1. 최신 게시 약관
    // ==========================================
    @Transactional(readOnly = true)
    public List<TermsResponse> current() {
        return currentDocuments()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private List<TermsDocument> currentDocuments() {
        LocalDateTime now =
                LocalDateTime.now(ZoneOffset.UTC);

        Map<String, TermsDocument> latest =
                new LinkedHashMap<>();

        // 신규 약관일수록 ID가 큰 구조
        for (TermsDocument doc :
                termsRepository.findByStatusOrderByIdDesc(
                        "PUBLISHED"
                )) {

            if (doc.getEffectiveAt() == null
                    || doc.getEffectiveAt().isAfter(now)) {
                continue;
            }

            latest.putIfAbsent(
                    doc.getTermsType(),
                    doc
            );
        }

        return List.of(
                        "TERMS",
                        "PRIVACY_NOTICE",
                        "MARKETING"
                ).stream()
                .map(latest::get)
                .filter(Objects::nonNull)
                .toList();
    }

    // ==========================================
    // 2. 관리자 전체 약관 목록
    // ==========================================
    @Transactional(readOnly = true)
    public List<TermsResponse> all() {
        return termsRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ==========================================
    // 3. 관리자 초안 생성
    // ==========================================
    @Transactional
    public TermsResponse createDraft(
            Long adminId,
            String type,
            String version,
            String title,
            String content
    ) {
        if (!Set.of(
                "TERMS",
                "PRIVACY_NOTICE",
                "MARKETING"
        ).contains(type)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 약관 종류입니다."
            );
        }

        if (termsRepository.existsByTermsTypeAndVersion(
                type, version.trim()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 존재하는 약관 버전입니다."
            );
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED
                        )
                );

        TermsDocument saved = termsRepository.saveAndFlush(
                TermsDocument.draft(
                        type,
                        version,
                        title,
                        content,
                        admin
                )
        );

        return toResponse(saved);
    }

    // ==========================================
    // 4. 관리자 초안 수정
    // ==========================================
    @Transactional
    public TermsResponse updateDraft(
            Long id,
            String title,
            String content
    ) {
        TermsDocument document = find(id);

        if (!"DRAFT".equals(document.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "게시된 약관은 수정할 수 없습니다."
            );
        }

        document.updateDraft(title, content);

        return toResponse(document);
    }

    // ==========================================
    // 5. 약관 게시
    // ==========================================
    @Transactional
    public TermsResponse publish(Long id) {
        TermsDocument document = find(id);

        if (!"DRAFT".equals(document.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "초안 상태의 약관만 게시할 수 있습니다."
            );
        }

        // 동일 종류의 이전 게시 약관은 보관
        List<TermsDocument> oldDocuments =
                termsRepository.findByTermsTypeAndStatus(
                        document.getTermsType(),
                        "PUBLISHED"
                );

        oldDocuments.forEach(TermsDocument::archive);

        document.publish(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        return toResponse(document);
    }

    private TermsDocument find(Long id) {
        return termsRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "약관을 찾을 수 없습니다."
                        )
                );
    }

    // ==========================================
    // 6. 회원가입 필수 약관 검증 + 동의 저장
    // AuthService.signUp 트랜잭션에서 호출
    // ==========================================
    @Transactional
    public void recordSignupAcceptances(
            User user,
            List<Long> acceptedIds
    ) {
        List<TermsDocument> current = currentDocuments();

        // 이용약관 자체가 아직 게시되지 않았다면
        // 회원가입을 진행하지 않음
        boolean hasTerms = current.stream()
                .anyMatch(doc ->
                        "TERMS".equals(doc.getTermsType())
                );

        if (!hasTerms) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "이용약관이 아직 게시되지 않았습니다."
            );
        }

        Set<Long> selected = acceptedIds == null
                ? Set.of()
                : new HashSet<>(acceptedIds);

        Set<Long> allowedIds = current.stream()
                .filter(doc ->
                        !"PRIVACY_NOTICE".equals(
                                doc.getTermsType()
                        )
                )
                .map(TermsDocument::getId)
                .collect(Collectors.toSet());

        // 임의 약관 ID, 과거 버전 동의 차단
        if (!allowedIds.containsAll(selected)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "약관이 변경되었습니다. 다시 확인해주세요."
            );
        }

        for (TermsDocument document : current) {
            if ("PRIVACY_NOTICE".equals(
                    document.getTermsType()
            )) {
                continue;
            }

            if (document.isRequired()
                    && !selected.contains(document.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "필수 이용약관에 동의해주세요."
                );
            }

            if (selected.contains(document.getId())) {
                acceptanceRepository.save(
                        UserTermsAcceptance.create(
                                user,
                                document,
                                "LOCAL"
                        )
                );
            }
        }
    }
}

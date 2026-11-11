package com.spoilock.backend.copyright.service;

import com.spoilock.backend.common.storage.S3StorageService;
import com.spoilock.backend.copyright.domain.dto.CopyrightEvidenceResponse;
import com.spoilock.backend.copyright.domain.entity.CopyrightEvidence;
import com.spoilock.backend.copyright.domain.entity.CopyrightLicense;
import com.spoilock.backend.copyright.repository.CopyrightEvidenceRepository;
import com.spoilock.backend.copyright.repository.CopyrightLicenseRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.enums.UserRole;
import com.spoilock.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CopyrightEvidenceService {

    private final CopyrightEvidenceRepository evidenceRepository;
    private final CopyrightLicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final S3StorageService storageService;

    public record EvidenceDownload(
            String filename,
            byte[] bytes
    ) {
    }

    private record ValidatedFile(
            String filename,
            String contentType,
            String extension,
            byte[] bytes,
            String sha256
    ) {
    }

    // =========================================
    // 1. 증빙 목록
    // =========================================
    public Page<CopyrightEvidenceResponse> getEvidences(
            Long actorId,
            Long licenseId,
            int page,
            int size
    ) {
        requireAdmin(actorId);
        findLicense(licenseId);

        if (page < 0 || size < 1 || size > 100) {
            throw badRequest(
                    "페이지는 0 이상, 조회 개수는 1~100이어야 합니다."
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

        return evidenceRepository
                .findByLicense_Id(licenseId, pageable)
                .map(CopyrightEvidenceResponse::from);
    }

    // =========================================
    // 2. 증빙 업로드
    // =========================================
    @Transactional
    public CopyrightEvidenceResponse upload(
            Long actorId,
            Long licenseId,
            MultipartFile file
    ) {
        User actor = requireAdmin(actorId);
        CopyrightLicense license = findLicense(licenseId);

        ValidatedFile validated = validateFile(file);

        // 원본 파일명을 저장 경로에 사용하지 않는다.
        String storageKey = "copyright/evidences/"
                + licenseId
                + "/"
                + UUID.randomUUID()
                + "."
                + validated.extension();

        CopyrightEvidence evidence;

        try {
            evidence = CopyrightEvidence.create(
                    license,
                    validated.filename(),
                    storageKey,
                    validated.contentType(),
                    validated.bytes().length,
                    validated.sha256(),
                    actor
            );
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }

        // 파일을 올리기 전에 트랜잭션 연결 여부 확인
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "증빙 업로드에는 트랜잭션이 필요합니다."
            );
        }

        // S3 업로드가 정상 완료된 경우에만 롤백 시 정리
        boolean[] uploaded = {false};

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (uploaded[0]
                                && status == STATUS_ROLLED_BACK) {
                            storageService.deleteQuietly(storageKey);
                        }
                    }
                }
        );

        storageService.uploadBytes(
                storageKey,
                validated.bytes(),
                validated.contentType()
        );

        uploaded[0] = true;

        CopyrightEvidence saved =
                evidenceRepository.saveAndFlush(evidence);

        return CopyrightEvidenceResponse.from(saved);
    }

    // =========================================
    // 3. 증빙 다운로드
    // =========================================
    public EvidenceDownload download(
            Long actorId,
            Long licenseId,
            Long evidenceId
    ) {
        requireAdmin(actorId);
        findLicense(licenseId);

        if (evidenceId == null || evidenceId <= 0) {
            throw badRequest("올바른 증빙 ID가 필요합니다.");
        }

        CopyrightEvidence evidence = evidenceRepository
                .findByIdAndLicense_Id(evidenceId, licenseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "증빙 파일을 찾을 수 없습니다."
                ));

        byte[] bytes = storageService.readBytes(
                evidence.getStorageKey()
        );

        // 저장 당시 기록과 실제 내려받은 파일이 같은지 검사
        if (bytes.length != evidence.getSizeBytes()
                || !sha256(bytes).equals(evidence.getSha256())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "저장된 증빙 파일의 무결성을 확인할 수 없습니다."
            );
        }

        return new EvidenceDownload(
                evidence.getOriginalFilename(),
                bytes
        );
    }

    // =========================================
    // 4. 파일 검증
    // =========================================
    private ValidatedFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("첨부할 파일을 선택해 주세요.");
        }

        if (file.getSize() > CopyrightEvidence.MAX_FILE_SIZE) {
            throw fileTooLarge();
        }

        String filename = normalizeFilename(
                file.getOriginalFilename()
        );

        String lowerName = filename.toLowerCase(Locale.ROOT);

        String contentType;
        String extension;
        byte[] expectedHeader;

        if (lowerName.endsWith(".pdf")) {
            contentType = "application/pdf";
            extension = "pdf";
            expectedHeader = new byte[]{
                    0x25, 0x50, 0x44, 0x46, 0x2D
            };
        } else if (lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
            extension = "jpg";
            expectedHeader = new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
            };
        } else if (lowerName.endsWith(".png")) {
            contentType = "image/png";
            extension = "png";
            expectedHeader = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A
            };
        } else {
            throw badRequest(
                    "PDF, JPG, PNG 파일만 첨부할 수 있습니다."
            );
        }

        byte[] bytes;

        // 요청에서 전달한 크기와 별개로 실제 읽는 양도 제한
        try (InputStream input = file.getInputStream()) {
            bytes = input.readNBytes(
                    (int) CopyrightEvidence.MAX_FILE_SIZE + 1
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "첨부 파일을 읽을 수 없습니다.",
                    exception
            );
        }

        if (bytes.length == 0) {
            throw badRequest("빈 파일은 첨부할 수 없습니다.");
        }

        if (bytes.length > CopyrightEvidence.MAX_FILE_SIZE) {
            throw fileTooLarge();
        }

        // 브라우저가 보낸 Content-Type을 그대로 신뢰하지 않는다.
        if (!startsWith(bytes, expectedHeader)) {
            throw badRequest(
                    "파일 확장자와 실제 파일 형식이 일치하지 않습니다."
            );
        }

        return new ValidatedFile(
                filename,
                contentType,
                extension,
                bytes,
                sha256(bytes)
        );
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw badRequest("파일명이 필요합니다.");
        }

        // 일부 클라이언트가 보낸 경로를 제거하고 파일명만 사용
        String normalized = originalFilename.replace('\\', '/');

        String filename = normalized.substring(
                normalized.lastIndexOf('/') + 1
        ).trim();

        if (filename.isBlank() || filename.length() > 255) {
            throw badRequest("파일명은 1~255자여야 합니다.");
        }

        if (filename.codePoints().anyMatch(Character::isISOControl)) {
            throw badRequest("사용할 수 없는 파일명입니다.");
        }

        return filename;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }

        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }

        return true;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(bytes)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256을 사용할 수 없습니다.",
                    exception
            );
        }
    }

    // =========================================
    // 5. 관리자 및 이용 권한 확인
    // =========================================
    private User requireAdmin(Long actorId) {
        if (actorId == null || actorId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "사용자 정보를 확인할 수 없습니다."
                ));

        if (actor.getAccountStatus() != AccountStatus.ACTIVE
                || actor.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "관리자만 이용할 수 있습니다."
            );
        }

        return actor;
    }

    private CopyrightLicense findLicense(Long licenseId) {
        if (licenseId == null || licenseId <= 0) {
            throw badRequest("올바른 이용 권한 ID가 필요합니다.");
        }

        return licenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "이용 권한 기록을 찾을 수 없습니다."
                ));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private static ResponseStatusException fileTooLarge() {
        return new ResponseStatusException(
                HttpStatus.valueOf(413),
                "증빙 파일은 10MB 이하여야 합니다."
        );
    }
}
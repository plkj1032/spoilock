package com.spoilock.backend.post.service;

import com.spoilock.backend.common.storage.S3StorageService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 게시글 이미지 저장소.
 * 새 이미지는 S3(feed-images/…)에 저장 → 서버가 여러 대여도 같은 파일을 보고, 판정 작업도 어느 서버에서나 가능.
 * storage_key 가 "s3:" 로 시작하지 않는 이전 이미지는 서버 디스크(app.feed-image-storage-dir)에서 읽는다.
 */
@Component
@RequiredArgsConstructor
public class PostMediaStorage {

    static final String S3_PREFIX = "s3:";
    private static final String S3_DIR = "feed-images/";

    private final S3StorageService s3StorageService;

    @Value("${app.feed-image-storage-dir:./private/feed-images}")
    private String legacyDir;

    /** 저장 후 storage_key 반환 */
    public String save(byte[] bytes, String contentType, String extension) {
        String objectKey = S3_DIR + UUID.randomUUID() + extension;
        s3StorageService.uploadBytes(objectKey, bytes, contentType);
        return S3_PREFIX + objectKey;
    }

    public byte[] read(String storageKey) {
        if (storageKey.startsWith(S3_PREFIX)) {
            return s3StorageService.readBytes(storageKey.substring(S3_PREFIX.length()));
        }
        try {
            return Files.readAllBytes(legacyPath(storageKey));
        } catch (NoSuchFileException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지 파일을 찾을 수 없습니다.", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지를 읽지 못했습니다.", ex);
        }
    }

    public void deleteQuietly(String storageKey) {
        if (storageKey.startsWith(S3_PREFIX)) {
            s3StorageService.deleteQuietly(storageKey.substring(S3_PREFIX.length()));
            return;
        }
        try {
            Files.deleteIfExists(legacyPath(storageKey));
        } catch (IOException | RuntimeException ignored) {
            // 정리 실패는 무시
        }
    }

    private Path legacyPath(String storageKey) {
        Path root = Path.of(legacyDir).toAbsolutePath().normalize();
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "잘못된 파일 경로입니다.");
        }
        return path;
    }
}

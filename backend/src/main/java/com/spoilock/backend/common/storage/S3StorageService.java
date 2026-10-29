
package com.spoilock.backend.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${app.media.s3.bucket}")
    private String bucket;

    // 자막 등 작은 파일 업로드
    // 기존 객체는 덮어쓰지 않음
    public void uploadBytes(
            String key,
            byte[] bytes,
            String contentType
    ) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .ifNoneMatch("*")
                            .build(),
                    RequestBody.fromBytes(bytes)
            );

            log.info("S3 업로드 완료: key={}", key);

        } catch (S3Exception ex) {
            throw storageError(ex);
        }
    }

    // MP4 등 대용량 파일 업로드
    // 파일 전체를 byte[]로 적재하지 않음
    public void uploadFile(
            String key,
            Path source,
            String contentType
    ) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .ifNoneMatch("*")
                            .build(),
                    RequestBody.fromFile(source)
            );

            log.info("S3 파일 업로드 완료: key={}", key);

        } catch (S3Exception ex) {
            throw storageError(ex);
        }
    }

    // SRT 등 작은 파일 다운로드
    public byte[] readBytes(String key) {
        try {
            ResponseBytes<GetObjectResponse> result =
                    s3Client.getObject(
                            GetObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(key)
                                    .build(),
                            ResponseTransformer.toBytes()
                    );

            return result.asByteArray();

        } catch (NoSuchKeyException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "S3에 파일이 없습니다.",
                    ex
            );

        } catch (S3Exception ex) {
            throw storageError(ex);
        }
    }

    // 영상 분석을 위한 임시 파일 다운로드
    // 반환된 파일은 사용 후 반드시 삭제해야 함
    public Path downloadToTemporaryFile(
            String key,
            String suffix
    ) {
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile(
                    "spoilock-s3-",
                    suffix
            );

            // S3의 toFile은 기존 파일을 덮어쓰지 않음
            Files.deleteIfExists(tempFile);

            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    ResponseTransformer.toFile(tempFile)
            );

            return tempFile;

        } catch (IOException | RuntimeException ex) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupEx) {
                    log.warn(
                            "임시 파일 정리 실패: {}",
                            tempFile,
                            cleanupEx
                    );
                }
            }

            if (ex instanceof NoSuchKeyException) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "S3에 영상 파일이 없습니다.",
                        ex
                );
            }

            if (ex instanceof S3Exception s3Ex) {
                throw storageError(s3Ex);
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "S3 영상 다운로드에 실패했습니다.",
                    ex
            );
        }
    }

    // S3 객체 삭제
    public void delete(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );

            log.info("S3 파일 삭제: key={}", key);

        } catch (S3Exception ex) {
            throw storageError(ex);
        }
    }

    // 트랜잭션 롤백 등의 정리 작업에서 사용
    public void deleteQuietly(String key) {
        try {
            delete(key);
        } catch (RuntimeException ex) {
            log.error(
                    "S3 파일 정리 실패: key={}",
                    key,
                    ex
            );
        }
    }

    private ResponseStatusException storageError(
            S3Exception ex
    ) {
        if (ex.statusCode() == 412
                || ex.statusCode() == 409) {
            return new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 파일이 존재하거나 업로드가 충돌했습니다.",
                    ex
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "S3 저장소 요청에 실패했습니다.",
                ex
        );
    }
}

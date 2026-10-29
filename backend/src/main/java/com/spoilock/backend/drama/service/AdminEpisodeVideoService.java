
package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.AdminEpisodeVideoResponse;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.entity.EpisodeVideo;
import com.spoilock.backend.common.storage.S3StorageService;
import com.spoilock.backend.drama.domain.dto.AdminEpisodeVideoResponse;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.entity.EpisodeVideo;
import com.spoilock.backend.drama.repository.EpisodeRepository;
import com.spoilock.backend.drama.repository.EpisodeVideoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEpisodeVideoService {

    private static final long MAX_SIZE =
            400L * 1024 * 1024;

    private final EpisodeRepository episodeRepository;
    private final EpisodeVideoRepository videoRepository;
    private final S3StorageService s3StorageService;

    private Episode findEpisode(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회차를 찾을 수 없습니다."
                ));
    }

    @Transactional(readOnly = true)
    public AdminEpisodeVideoResponse getVideo(Long episodeId) {

        findEpisode(episodeId);

        return videoRepository.findByEpisode_Id(episodeId)
                .map(AdminEpisodeVideoResponse::from)
                .orElseGet(() ->
                        AdminEpisodeVideoResponse.empty(episodeId)
                );
    }

    @Transactional
    public AdminEpisodeVideoResponse upload(
            Long episodeId,
            MultipartFile file
    ) {
        Episode episode = findEpisode(episodeId);

        if (videoRepository.findByEpisode_Id(episodeId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 영상이 등록된 회차입니다."
            );
        }

        if (file == null
                || file.isEmpty()
                || file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "MP4 파일은 400MB 이하로 등록해 주세요."
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일명이 없습니다."
            );
        }

        filename = filename.replace('\\', '/');
        filename = filename.substring(
                filename.lastIndexOf('/') + 1
        );

        if (filename.isBlank()
                || filename.length() > 255
                || !filename.toLowerCase(Locale.ROOT)
                .endsWith(".mp4")
                || filename.chars().anyMatch(
                Character::isISOControl
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 MP4 파일을 선택해 주세요."
            );
        }

        Path tempFile = null;

        try {
            // 원본 영상 전체를 Java 메모리에 올리지 않음
            tempFile = Files.createTempFile(
                    "spoilock-video-upload-",
                    ".mp4"
            );

            try (InputStream input = file.getInputStream()) {
                Files.copy(
                        input,
                        tempFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            long size = Files.size(tempFile);

            if (size == 0 || size > MAX_SIZE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "영상 크기가 허용 범위를 벗어났습니다."
                );
            }

            if (!hasMp4Header(tempFile)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "유효한 MP4 파일이 아닙니다."
                );
            }

            String storageKey = UUID.randomUUID() + ".mp4";
            String s3Key = "videos/" + storageKey;

            // S3에 먼저 저장
            s3StorageService.uploadFile(
                    s3Key,
                    tempFile,
                    "video/mp4"
            );

            // DB 저장이 롤백되면 S3 객체 정리
            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCompletion(int status) {
                                    if (status != STATUS_COMMITTED) {
                                        s3StorageService.deleteQuietly(s3Key);
                                    }
                                }
                            }
                    );

            EpisodeVideo video = EpisodeVideo.create(
                    episode,
                    filename,
                    storageKey,
                    size
            );

            EpisodeVideo saved;

            try {
                saved = videoRepository.saveAndFlush(video);
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 영상이 등록되어 있습니다.",
                        ex
                );
            }

            log.info(
                    "S3 MP4 등록: episodeId={}, key={}, bytes={}",
                    episodeId,
                    s3Key,
                    size
            );

            return AdminEpisodeVideoResponse.from(saved);

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "영상 파일 처리 중 오류가 발생했습니다.",
                    ex
            );

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ex) {
                    log.warn("임시 영상 삭제 실패: {}", tempFile, ex);
                }
            }
        }
    }

    private boolean hasMp4Header(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            byte[] header = input.readNBytes(12);

            return header.length >= 12
                    && header[4] == 'f'
                    && header[5] == 't'
                    && header[6] == 'y'
                    && header[7] == 'p';
        }
    }

    @Transactional
    public void delete(Long episodeId) {

        findEpisode(episodeId);

        EpisodeVideo video = videoRepository
                .findByEpisode_Id(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "등록된 영상이 없습니다."
                ));

        String key = "videos/" + video.getStorageKey();

        videoRepository.delete(video);
        videoRepository.flush();

        // DB 커밋 이후 파일 삭제
        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                s3StorageService.deleteQuietly(key);
                            }
                        }
                );

        log.info("S3 MP4 삭제: episodeId={}", episodeId);
    }
}

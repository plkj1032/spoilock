
package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.AdminEpisodeVideoResponse;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.entity.EpisodeVideo;
import com.spoilock.backend.drama.repository.EpisodeRepository;
import com.spoilock.backend.drama.repository.EpisodeVideoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
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
import java.nio.file.*;
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

    @Value("${app.video-storage-dir:./private/videos}")
    private String videoStorageDir;

    private Episode findEpisode(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회차를 찾을 수 없습니다."
                ));
    }

    private Path getStorageDirectory() {
        return Path.of(videoStorageDir)
                .toAbsolutePath()
                .normalize();
    }

    // ==========================================
    // 1. 영상 등록 정보 조회
    // ==========================================
    @Transactional(readOnly = true)
    public AdminEpisodeVideoResponse getVideo(Long episodeId) {

        findEpisode(episodeId);

        return videoRepository.findByEpisode_Id(episodeId)
                .map(AdminEpisodeVideoResponse::from)
                .orElseGet(() ->
                        AdminEpisodeVideoResponse.empty(episodeId)
                );
    }

    // ==========================================
    // 2. MP4 영상 업로드
    // ==========================================
    @Transactional
    public AdminEpisodeVideoResponse upload(
            Long episodeId,
            MultipartFile file
    ) {
        Episode episode = findEpisode(episodeId);

        // 이미 등록된 영상이 있으면 중복 차단
        if (videoRepository
                .findByEpisode_Id(episodeId)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 영상이 등록된 회차입니다."
            );
        }

        if (file == null ||
                file.isEmpty() ||
                file.getSize() > MAX_SIZE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "MP4 파일은 100MB 이하로 등록해 주세요."
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일명이 존재하지 않습니다."
            );
        }

        filename = filename.replace('\\', '/');
        filename = filename.substring(
                filename.lastIndexOf('/') + 1
        );

        if (filename.isBlank() ||
                filename.length() > 255 ||
                !filename.toLowerCase(Locale.ROOT)
                        .endsWith(".mp4") ||
                filename.chars().anyMatch(
                        Character::isISOControl
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "MP4 파일만 업로드할 수 있습니다."
            );
        }

        Path directory = getStorageDirectory();

        Path tempFile = null;
        Path savedFile = null;

        try {
            Files.createDirectories(directory);

            // 임시 파일 생성
            tempFile = Files.createTempFile(
                    directory,
                    "video-upload-",
                    ".tmp"
            );

            // 스트림 복사: 전체 영상을 메모리에 적재하지 않음
            try (InputStream input = file.getInputStream()) {
                Files.copy(
                        input,
                        tempFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            long actualSize = Files.size(tempFile);

            if (actualSize == 0 || actualSize > MAX_SIZE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "영상 크기가 허용 범위를 벗어났습니다."
                );
            }

            // 실제 파일의 MP4 헤더 기본 확인
            if (!hasMp4Header(tempFile)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "유효한 MP4 파일 헤더를 찾지 못했습니다."
                );
            }

            // 브라우저 파일명을 실제 저장 경로에 사용하지 않음
            String storageKey = UUID.randomUUID() + ".mp4";

            savedFile = directory.resolve(storageKey);

            // 임시 파일을 최종 파일로 이동
            try {
                Files.move(
                        tempFile,
                        savedFile,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, savedFile);
            }

            tempFile = null;

            // DB 롤백 시 최종 영상 파일 정리
            registerRollbackCleanup(savedFile);

            EpisodeVideo video = EpisodeVideo.create(
                    episode,
                    filename,
                    storageKey,
                    actualSize
            );

            EpisodeVideo saved;

            try {
                saved = videoRepository.saveAndFlush(video);
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 영상이 등록된 회차이거나 "
                                + "저장 데이터가 중복되었습니다.",
                        ex
                );
            }

            log.info(
                    "MP4 업로드: episodeId={}, storageKey={}, bytes={}",
                    episodeId,
                    storageKey,
                    actualSize
            );

            return AdminEpisodeVideoResponse.from(saved);

        } catch (IOException ex) {

            if (savedFile != null) {
                deleteQuietly(savedFile);
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "영상 파일 저장 중 오류가 발생했습니다.",
                    ex
            );

        } catch (RuntimeException ex) {

            if (savedFile != null) {
                deleteQuietly(savedFile);
            }

            throw ex;

        } finally {

            // 실패한 임시 업로드 파일 정리
            if (tempFile != null) {
                deleteQuietly(tempFile);
            }
        }
    }

    // ==========================================
    // MP4 파일 시그니처 기본 검사
    // 일반적인 ISO BMFF MP4는 4~7바이트에 ftyp
    // ==========================================
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

    // ==========================================
    // 3. 등록된 영상 삭제
    // ==========================================
    @Transactional
    public void delete(Long episodeId) {

        findEpisode(episodeId);

        EpisodeVideo video = videoRepository
                .findByEpisode_Id(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "등록된 영상이 없습니다."
                ));

        Path target = getStorageDirectory()
                .resolve(video.getStorageKey())
                .normalize();

        // DB 삭제
        videoRepository.delete(video);
        videoRepository.flush();

        // DB 커밋 이후 영상 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        deleteQuietly(target);
                    }
                }
        );

        log.info("MP4 삭제 요청: episodeId={}", episodeId);
    }

    // ==========================================
    // 업로드 트랜잭션 롤백 시 파일 정리
    // ==========================================
    private void registerRollbackCleanup(Path target) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status != STATUS_COMMITTED) {
                            deleteQuietly(target);
                        }
                    }
                }
        );
    }

    private void deleteQuietly(Path path) {

        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.error("영상 파일 정리 실패: {}", path, ex);
        }
    }
}

package com.spoilock.backend.spoiler.service;

import com.spoilock.backend.common.storage.S3StorageService;
import com.spoilock.backend.drama.entity.Episode;
import com.spoilock.backend.drama.repository.EpisodeRepository;
import com.spoilock.backend.spoiler.dto.*;
import com.spoilock.backend.spoiler.entity.EpisodeAnalysis;
import com.spoilock.backend.spoiler.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEpisodeAiService {

    private final S3StorageService s3StorageService;

    private static final long MAX_SUBTITLE_SIZE =
            2L * 1024 * 1024;

    private final EpisodeRepository episodeRepository;

    private final EpisodeAnalysisRepository analysisRepository;

    @Value("${app.subtitle-storage-dir:./private/subtitles}")
    private String subtitleStorageDir;


    // ============================================
    // 공통: 회차 조회
    // ============================================
    private Episode findEpisode(Long episodeId) {

        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회차를 찾을 수 없습니다."
                ));
    }


    // ============================================
    // 1. 회차별 AI 분석 상태 조회
    // ============================================
    @Transactional(readOnly = true)
    public AdminEpisodeAiResponse getAnalysis(Long episodeId) {

        findEpisode(episodeId);

        return analysisRepository.findByEpisode_Id(episodeId)
                .map(AdminEpisodeAiResponse::from)
                .orElseGet(() ->
                        AdminEpisodeAiResponse.empty(episodeId)
                );
    }


    // ============================================
    // 2. 회차별 SRT 자막 업로드
    // ============================================

    @Transactional
    public AdminEpisodeAiResponse uploadSubtitle(
            Long episodeId,
            MultipartFile file
    ) {
        Episode episode = findEpisode(episodeId);

        if (file == null
                || file.isEmpty()
                || file.getSize() > MAX_SUBTITLE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자막 파일은 2MB 이하로 등록해 주세요."
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
                || !filename.toLowerCase(Locale.ROOT).endsWith(".srt")
                || filename.chars().anyMatch(Character::isISOControl)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 SRT 파일을 선택해 주세요."
            );
        }

        EpisodeAnalysis existing = analysisRepository
                .findByEpisode_Id(episodeId)
                .orElse(null);

        if (existing != null
                && existing.getSubtitleFilename() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 자막이 등록된 회차입니다."
            );
        }

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자막 파일을 읽을 수 없습니다.",
                    ex
            );
        }

        // UTF-8 검사
        try {
            String content = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();

            if (!content.contains("-->")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "SRT 시간 구간이 없습니다."
                );
            }

        } catch (CharacterCodingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "UTF-8 자막만 지원합니다.",
                    ex
            );
        }

        // 두 EC2가 동일한 S3 객체를 사용
        String key = "subtitles/" + episodeId + ".srt";

        s3StorageService.uploadBytes(
                key,
                bytes,
                "application/x-subrip"
        );

        // DB 트랜잭션이 롤백되면 S3 객체 정리
        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                if (status != STATUS_COMMITTED) {
                                    s3StorageService.deleteQuietly(key);
                                }
                            }
                        }
                );

        EpisodeAnalysis analysis;

        if (existing == null) {
            analysis = EpisodeAnalysis.create(
                    episode,
                    filename
            );
        } else {
            existing.attachSubtitle(filename);
            analysis = existing;
        }

        analysisRepository.saveAndFlush(analysis);

        log.info(
                "S3 자막 등록 완료: episodeId={}, key={}",
                episodeId,
                key
        );

        return AdminEpisodeAiResponse.from(analysis);
    }



    // ============================================
    // 트랜잭션 롤백 시 저장 파일 정리
    // ============================================
    private void registerRollbackCleanup(Path target) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        // 커밋 성공이 아닐 때만 삭제
                        if (status !=
                                TransactionSynchronization.STATUS_COMMITTED) {

                            log.warn(
                                    "DB 트랜잭션 미커밋: 자막 파일 정리 시도 {}",
                                    target
                            );

                            deleteFileQuietly(target);
                        }
                    }
                }
        );
    }


    // ============================================
    // 파일 삭제 보조 메서드
    // ============================================
    private void deleteFileQuietly(Path target) {

        try {

            Files.deleteIfExists(target);

        } catch (IOException ex) {

            log.error(
                    "자막 파일 정리 실패: {}",
                    target,
                    ex
            );
        }
    }


    // ============================================
    // 3. 자막 삭제 (교체 업로드용)
    //    Python 에 등록된 사건은 그대로 남음 → 새 자막은 재분석(replace)으로 요청
    //    분석 요청은 SubtitleAnalysisService, 사건 관리는 SpoilerEventAdminService
    // ============================================

    @Transactional
    public AdminEpisodeAiResponse deleteSubtitle(Long episodeId) {

        findEpisode(episodeId);

        EpisodeAnalysis analysis = analysisRepository
                .findByEpisode_Id(episodeId)
                .filter(a -> a.getSubtitleFilename() != null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "등록된 자막이 없습니다."
                ));

        if (analysis.isJobActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "분석 중인 자막은 삭제할 수 없습니다."
            );
        }

        analysis.detachSubtitle();

        String key = "subtitles/" + episodeId + ".srt";

        // DB 커밋 이후 S3 파일 삭제
        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                s3StorageService.deleteQuietly(key);
                            }
                        }
                );

        return AdminEpisodeAiResponse.from(analysis);
    }

}

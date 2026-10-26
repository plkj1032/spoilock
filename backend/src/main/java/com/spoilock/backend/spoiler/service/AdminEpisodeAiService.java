
package com.spoilock.backend.spoiler.service;

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

        // 1) 회차 존재 여부 확인
        Episode episode = findEpisode(episodeId);

        // 2) 파일 존재 여부 및 용량 검사
        if (file == null
                || file.isEmpty()
                || file.getSize() > MAX_SUBTITLE_SIZE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자막 파일은 2MB 이하로 등록해 주세요."
            );
        }

        // 3) 원본 파일명 조회
        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일명이 존재하지 않습니다."
            );
        }

        // 업로드 파일명에 경로가 들어오는 경우 제거
        filename = filename.replace('\\', '/');

        filename = filename.substring(
                filename.lastIndexOf('/') + 1
        );

        // 4) 확장자 및 파일명 검증
        if (filename.isBlank()
                || filename.length() > 255
                || !filename.toLowerCase(Locale.ROOT).endsWith(".srt")
                || filename.chars().anyMatch(
                Character::isISOControl
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 SRT 파일을 선택해 주세요."
            );
        }

        // 5) 기존 자막 등록 여부 검사
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

        // 6) 파일을 byte[]로 읽기
        byte[] bytes;

        try {
            bytes = file.getBytes();

        } catch (IOException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일을 읽을 수 없습니다.",
                    ex
            );
        }

        // 7) UTF-8 및 SRT 기본 형식 검사
        try {

            String content = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(
                            CodingErrorAction.REPORT
                    )
                    .onUnmappableCharacter(
                            CodingErrorAction.REPORT
                    )
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();

            if (!content.contains("-->")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "SRT 시간 구간 정보가 없습니다."
                );
            }

        } catch (CharacterCodingException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "현재는 UTF-8 자막만 지원합니다.",
                    ex
            );
        }

        // 8) 자막 저장 디렉터리 생성
        Path directory = Path.of(subtitleStorageDir)
                .toAbsolutePath()
                .normalize();

        // 사용자가 보낸 파일명이 아닌 회차 ID로 저장
        Path target = directory.resolve(
                episodeId + ".srt"
        );

        boolean fileCreated = false;

        try {

            Files.createDirectories(directory);

            // 9) 파일을 독점 생성
            // 이미 존재하면 FileAlreadyExistsException 발생
            Files.createFile(target);

            fileCreated = true;

            // 10) 파일 내용 저장
            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            // 11) DB에 분석 정보 생성 또는 갱신
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

            // 12) DB에 INSERT 또는 UPDATE 실행
            analysisRepository.saveAndFlush(analysis);

            // 13) 트랜잭션 롤백 시 파일 정리
            registerRollbackCleanup(target);

            log.info(
                    "SRT 업로드 완료: episodeId={}, filename={}",
                    episodeId,
                    filename
            );

            return AdminEpisodeAiResponse.from(analysis);

        } catch (FileAlreadyExistsException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "서버에 해당 회차의 자막 파일이 이미 존재합니다.",
                    ex
            );

        } catch (IOException ex) {

            // 파일이 생성되었다면 삭제 시도
            if (fileCreated) {
                deleteFileQuietly(target);
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "자막 파일 저장 중 오류가 발생했습니다.",
                    ex
            );

        } catch (RuntimeException ex) {

            // DB 예외 등이 발생한 경우 파일 삭제 시도
            if (fileCreated) {
                deleteFileQuietly(target);
            }

            // 원래 예외를 유지
            throw ex;
        }
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
                    "분석이 진행 중인 자막은 삭제할 수 없습니다."
            );
        }

        analysis.detachSubtitle();

        Path target = Path.of(subtitleStorageDir)
                .toAbsolutePath()
                .normalize()
                .resolve(episodeId + ".srt");

        // DB 커밋 이후 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        deleteFileQuietly(target);
                    }
                }
        );

        log.info("자막 삭제: episodeId={}", episodeId);

        return AdminEpisodeAiResponse.from(analysis);
    }
}


package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.ProgressResponse;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.UserProgress;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaRepository;
import com.spoilock.backend.drama.repository.EpisodeRepository;
import com.spoilock.backend.drama.repository.UserProgressRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final UserProgressRepository progressRepository;
    private final DramaRepository dramaRepository;
    private final EpisodeRepository episodeRepository;
    private final UserRepository userRepository;

    // 내 시청 진도 조회
    @Transactional(readOnly = true)
    public ProgressResponse getProgress(
            Long userId,
            Long dramaId
    ) {
        Drama drama = findPublishedDrama(dramaId);

        int watched = progressRepository
                .findByUser_IdAndDrama_Id(userId, dramaId)
                .map(UserProgress::getWatchedEpisodeNumber)
                .orElse(0);

        return new ProgressResponse(
                dramaId,
                watched,
                drama.getTotalEpisodeCount()
        );
    }

    // 내 시청 진도 등록 또는 변경
    @Transactional
    public ProgressResponse updateProgress(
            Long userId,
            Long dramaId,
            Integer watched
    ) {
        Drama drama = findPublishedDrama(dramaId);

        Integer total = drama.getTotalEpisodeCount();

        // 작품의 전체 회차 범위 검사
        if (total == null || watched < 0 || watched > total) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "유효하지 않은 시청 회차입니다."
            );
        }

        // 실제 등록된 회차인지 확인
        if (watched > 0 &&
                !episodeRepository
                        .existsByDrama_IdAndEpisodeNumber(
                                dramaId,
                                watched
                        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "등록되지 않은 회차입니다."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "사용자를 찾을 수 없습니다."
                        )
                );

        // 기존 데이터가 없으면 생성
        UserProgress progress = progressRepository
                .findByUser_IdAndDrama_Id(userId, dramaId)
                .orElseGet(() ->
                        UserProgress.create(user, drama, 0)
                );

        // 시청 진도 변경
        progress.updateWatchedEpisodeNumber(watched);

        // 신규면 INSERT, 기존이면 변경사항 저장
        progressRepository.save(progress);

        return new ProgressResponse(
                dramaId,
                watched,
                total
        );
    }

    private Drama findPublishedDrama(Long dramaId) {
        return dramaRepository
                .findByIdAndStatus(
                        dramaId,
                        DramaStatus.PUBLISHED
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "드라마를 찾을 수 없습니다."
                        )
                );
    }
}

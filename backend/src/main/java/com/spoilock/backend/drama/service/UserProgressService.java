package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.ProgressResponse;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.UserProgress;
import com.spoilock.backend.drama.repository.UserProgressQueryRepository;
import com.spoilock.backend.drama.repository.UserProgressRepository;
import com.spoilock.backend.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final UserProgressRepository progressRepository;
    private final UserProgressQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(
            Long userId,
            Long dramaId
    ) {
        Drama drama = findPublishedDrama(dramaId);

        int watched = queryRepository
                .findProgress(userId, dramaId)
                .map(UserProgress::getWatchedEpisodeNumber)
                .orElse(0);

        return new ProgressResponse(
                dramaId,
                watched,
                drama.getTotalEpisodeCount()
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ProgressResponse updateProgress(
            Long userId,
            Long dramaId,
            Integer watched
    ) {
        // 동일 사용자의 진도 변경 요청을 순서대로 처리
        User user = queryRepository.findLockedUser(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "사용자를 찾을 수 없습니다."
                ));

        Drama drama = findPublishedDrama(dramaId);
        Integer total = drama.getTotalEpisodeCount();

        if (watched == null
                || total == null
                || watched < 0
                || watched > total) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "유효하지 않은 시청 회차입니다."
            );
        }

        if (watched > 0
                && !queryRepository.existsEpisode(dramaId, watched)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "등록되지 않은 회차입니다."
            );
        }

        UserProgress progress = queryRepository
                .findProgress(userId, dramaId)
                .orElseGet(() -> UserProgress.create(user, drama, 0));

        int current = progress.getWatchedEpisodeNumber();

        if (drama.isStreamingProvided() && watched < current) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "영상 제공 작품은 시청 진도를 낮출 수 없습니다. "
                            + "현재 " + current + "화까지 설정되어 있습니다."
            );
        }

        progress.updateWatchedEpisodeNumber(watched);
        progressRepository.save(progress);

        return new ProgressResponse(
                dramaId,
                watched,
                total
        );
    }

    private Drama findPublishedDrama(Long dramaId) {
        return queryRepository.findPublishedDrama(dramaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "드라마를 찾을 수 없습니다."
                ));
    }
}
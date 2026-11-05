package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.DramaRatingResponse;
import com.spoilock.backend.drama.domain.dto.SaveDramaRatingRequest;
import com.spoilock.backend.drama.domain.entity.Drama;
import com.spoilock.backend.drama.domain.entity.DramaRating;
import com.spoilock.backend.drama.domain.entity.UserProgress;
import com.spoilock.backend.drama.repository.DramaRatingQueryRepository;
import com.spoilock.backend.drama.repository.DramaRatingRepository;
import com.spoilock.backend.drama.repository.UserProgressQueryRepository;
import com.spoilock.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class DramaRatingService {

    private final DramaRatingRepository ratingRepository;
    private final DramaRatingQueryRepository ratingQueryRepository;
    private final UserProgressQueryRepository progressQueryRepository;

    public DramaRatingResponse get(
            Long userId,
            Long dramaId
    ){
        requirePublishedDrama(dramaId);
        return buildResponse(userId, dramaId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DramaRatingResponse save(
            Long userId,
            Long dramaId,
            SaveDramaRatingRequest request
    ){
        Integer score = request.score();

        if(score == null || score < 0 || score > 10){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "별점 점수를 0부터 10 사이여야 합니다."
            );
        }

        User user = requireLockedUser(userId);
        Drama drama = requirePublishedDrama(dramaId);

        if (!hasWatched(userId, dramaId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "1화 이상 시청 진도를 설정한 후 평가할 수 있습니다."
            );
        }

        DramaRating rating = ratingQueryRepository
                .findMyRating(userId, dramaId)
                .orElse(null);

        if (rating == null) {
            ratingRepository.save(
                    DramaRating.create(user, drama, score)
            );
        } else {
            rating.changeScore(score);
        }

        // 변경을 DB에 반영한 다음 평균을 다시 계산
        ratingRepository.flush();

        return buildResponse(userId, dramaId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DramaRatingResponse delete(
            Long userId,
            Long dramaId
    ) {
        requireLockedUser(userId);
        requirePublishedDrama(dramaId);

        // 진도를 낮췄더라도 기존 내 평가는 삭제할 수 있다.
        ratingQueryRepository
                .findMyRating(userId, dramaId)
                .ifPresent(ratingRepository::delete);

        ratingRepository.flush();

        return buildResponse(userId, dramaId);
    }

    private DramaRatingResponse buildResponse(
            Long userId,
            Long dramaId
    ) {
        DramaRatingQueryRepository.Summary summary =
                ratingQueryRepository.summarize(dramaId);

        Integer myScore = userId == null
                ? null
                : ratingQueryRepository
                .findMyRating(userId, dramaId)
                .map(DramaRating::getScore)
                .orElse(null);

        return new DramaRatingResponse(
                dramaId,
                summary.averageRating(),
                summary.ratingCount(),
                myScore,
                userId != null && hasWatched(userId, dramaId)
        );
    }

    private boolean hasWatched(Long userId, Long dramaId) {
        Integer watchedEpisode = progressQueryRepository
                .findProgress(userId, dramaId)
                .map(UserProgress::getWatchedEpisodeNumber)
                .orElse(0);

        return watchedEpisode >= 1;
    }

    private Drama requirePublishedDrama(Long dramaId) {
        return progressQueryRepository
                .findPublishedDrama(dramaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공개된 작품을 찾을 수 없습니다."
                ));
    }

    private User requireLockedUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return progressQueryRepository
                .findLockedUser(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }
}

package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.PlaybackPositionResponse;
import com.spoilock.backend.drama.domain.dto.SavePlaybackPositionRequest;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.entity.EpisodePlaybackPosition;
import com.spoilock.backend.drama.repository.DramaWatchQueryRepository;
import com.spoilock.backend.drama.repository.EpisodePlaybackPositionQueryRepository;
import com.spoilock.backend.drama.repository.EpisodePlaybackPositionRepository;
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
@Transactional(readOnly = true)
public class EpisodePlaybackPositionService {

    private final EpisodePlaybackPositionRepository positionRepository;
    private final EpisodePlaybackPositionQueryRepository positionQueryRepository;
    private final DramaWatchQueryRepository watchQueryRepository;
    private final UserProgressQueryRepository userProgressQueryRepository;

    public PlaybackPositionResponse getLatestPosition(
            Long userId,
            Long dramaId
    ){
        if(!watchQueryRepository.isWatchableDrama(dramaId)){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "시청할 수 있는 작품을 찾을 수 없습니다."
            );
        }
        return positionQueryRepository
                .findLatestPosition(userId, dramaId)
                .map(position -> toResponse(
                        position.getEpisode().getId(),
                        position
                ))
                .orElseGet(()-> new PlaybackPositionResponse(
                        null,
                        false,
                        0,
                        null,
                        null
                ));
    }

    public PlaybackPositionResponse getPosition(
            Long userId,
            Long dramaId,
            Long episodeId
    ) {
        findPlayableEpisode(dramaId, episodeId);

        return positionQueryRepository
                .findPosition(userId, episodeId)
                .map(position -> toResponse(episodeId, position))
                .orElseGet(() -> new PlaybackPositionResponse(
                        episodeId,
                        false,
                        0,
                        null,
                        null
                ));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PlaybackPositionResponse savePosition(
            Long userId,
            Long dramaId,
            Long episodeId,
            SavePlaybackPositionRequest request
    ) {
        validatePosition(request);

        // 최초 저장 요청이 동시에 들어와도 중복 행이 생성되지 않도록 잠금
        User user = userProgressQueryRepository
                .findLockedUser(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        Episode episode = findPlayableEpisode(dramaId, episodeId);

        EpisodePlaybackPosition position = positionQueryRepository
                .findPosition(userId, episodeId)
                .orElse(null);

        if (position == null) {
            position = EpisodePlaybackPosition.create(
                    user,
                    episode,
                    request.positionSeconds(),
                    request.durationSeconds()
            );

            positionRepository.save(position);
        } else {
            position.updatePosition(
                    request.positionSeconds(),
                    request.durationSeconds()
            );
        }

        return toResponse(episodeId, position);
    }

    private Episode findPlayableEpisode(
            Long dramaId,
            Long episodeId
    ) {
        return watchQueryRepository
                .findPlayableVideo(dramaId, episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "재생 가능한 회차를 찾을 수 없습니다."
                ))
                .getEpisode();
    }

    private void validatePosition(SavePlaybackPositionRequest request) {
        Double position = request.positionSeconds();
        Double duration = request.durationSeconds();

        if (position == null
                || duration == null
                || !Double.isFinite(position)
                || !Double.isFinite(duration)
                || duration <= 0
                || position < 0
                || position > duration) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "재생 위치는 0초 이상, 영상 길이 이하여야 합니다."
            );
        }
    }

    private PlaybackPositionResponse toResponse(
            Long episodeId,
            EpisodePlaybackPosition position
    ) {
        return new PlaybackPositionResponse(
                episodeId,
                true,
                position.getPositionSeconds(),
                position.getDurationSeconds(),
                position.getUpdatedAt()
        );
    }
}
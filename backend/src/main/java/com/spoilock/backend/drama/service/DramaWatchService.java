package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.EpisodePlaybackResponse;
import com.spoilock.backend.drama.domain.dto.WatchEpisodeResponse;
import com.spoilock.backend.drama.domain.entity.Episode;
import com.spoilock.backend.drama.domain.entity.EpisodeVideo;
import com.spoilock.backend.drama.repository.DramaWatchQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DramaWatchService {

    private final DramaWatchQueryRepository queryRepository;
    private final S3Presigner s3Presigner;

    @Value("${app.media.s3.bucket}")
    private String bucket;

    public List<WatchEpisodeResponse> getEpisodes(Long dramaId) {
        if (!queryRepository.isWatchableDrama(dramaId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "시청할 수 있는 작품을 찾을 수 없습니다."
            );
        }

        return queryRepository.findEpisodes(dramaId);
    }

    public EpisodePlaybackResponse getPlayback(
            Long dramaId,
            Long episodeId
    ) {
        EpisodeVideo video = queryRepository
                .findPlayableVideo(dramaId, episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "재생 가능한 영상이 없습니다."
                ));

        Episode episode = video.getEpisode();

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key("videos/" + video.getStorageKey())
                .responseContentType("video/mp4")
                .responseContentDisposition("inline")
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(2))
                        .getObjectRequest(objectRequest)
                        .build();

        PresignedGetObjectRequest signed =
                s3Presigner.presignGetObject(presignRequest);

        return new EpisodePlaybackResponse(
                dramaId,
                episode.getId(),
                episode.getEpisodeNumber(),
                episode.getTitle(),
                signed.url().toString(),
                signed.expiration()
        );
    }
}
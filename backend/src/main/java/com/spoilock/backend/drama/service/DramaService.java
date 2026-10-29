package com.spoilock.backend.drama.service;

import com.spoilock.backend.drama.domain.dto.DramaDetailResponse;
import com.spoilock.backend.drama.domain.dto.DramaResponse;
import com.spoilock.backend.drama.domain.dto.EpisodeResponse;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaQueryRepository;
import com.spoilock.backend.drama.repository.DramaRepository;
import com.spoilock.backend.drama.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DramaService {

    private final DramaRepository dramaRepository;
    private final EpisodeRepository episodeRepository;
    private final DramaQueryRepository dramaQueryRepository;

    @Transactional(readOnly = true)
    public List<DramaResponse> getDramas()
    {
        return dramaRepository
                .findByStatusOrderByIdDesc(
                        DramaStatus.PUBLISHED
                )
                .stream()
                .map(DramaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DramaDetailResponse getDrama(Long dramaId) {
        return dramaQueryRepository
                .findPublishedDetail(dramaId)
                .map(DramaDetailResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "드라마를 찾을 수 없습니다."
                ));
    }

    // 해당 드라마의 회차 목록
    @Transactional(readOnly = true)
    public List<EpisodeResponse> getEpisodes(Long dramaId)
    {
        // 공개된 드라마인지 확인
        getDrama(dramaId);

        return episodeRepository
                .findByDrama_IdOrderByEpisodeNumberAsc(dramaId)
                .stream()
                .map(EpisodeResponse::from)
                .toList();
    }
}

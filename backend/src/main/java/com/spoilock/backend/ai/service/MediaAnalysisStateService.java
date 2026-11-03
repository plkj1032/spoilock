package com.spoilock.backend.ai.service;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.post.entity.PostMedia;
import com.spoilock.backend.post.repository.PostMediaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이미지 판정 상태 저장 전용. 메서드마다 짧은 독립 트랜잭션.
 * Python 호출 구간에는 DB 트랜잭션을 걸지 않기 위해 별도 Bean 으로 분리.
 */
@Service
@RequiredArgsConstructor
public class MediaAnalysisStateService {

    private final PostMediaRepository mediaRepository;

    /** 판정에 필요한 값 (트랜잭션 밖에서 쓰도록 엔티티 대신 값만) */
    public record Target(Long mediaId, Long dramaId, String storageKey, String contentType) {
    }

    /** PENDING/FAILED → PROCESSING (시도 횟수 +1). 이미 다른 작업이 가져갔으면 false */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryStart(Long mediaId) {
        return mediaRepository.startAnalysis(
                mediaId,
                List.of(AnalysisStatus.PENDING, AnalysisStatus.FAILED),
                AnalysisStatus.PROCESSING,
                LocalDateTime.now()
        ) == 1;
    }

    /** 멈춘 PROCESSING → PENDING (복구용) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseStuck(Long mediaId) {
        return mediaRepository.transitionAnalysisStatus(
                mediaId, AnalysisStatus.PROCESSING, AnalysisStatus.PENDING
        ) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Target load(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .map(m -> new Target(
                        m.getId(),
                        m.getPost().getDrama().getId(),
                        m.getStorageKey(),
                        m.getMediaType()
                ))
                .orElse(null);
    }

    /** 판정 결과 반영. PROCESSING 이 아니면(관리자 검수·다른 작업이 처리함) 무시 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean apply(Long mediaId, ImageVerdictPolicy.ImageDecision decision) {
        PostMedia media = mediaRepository.findById(mediaId).orElse(null);

        if (media == null || media.getAnalysisStatus() != AnalysisStatus.PROCESSING) {
            return false;
        }

        if (decision.failed()) {
            media.failAnalysis(decision.note());
        } else {
            media.completeAnalysis(decision.requiredEpisode(), decision.note());
        }
        return true;
    }
}

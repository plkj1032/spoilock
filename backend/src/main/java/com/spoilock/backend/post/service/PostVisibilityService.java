package com.spoilock.backend.post.service;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.repository.UserProgressRepository;
import com.spoilock.backend.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostVisibilityService {

    private final UserProgressRepository progressRepository;

    public boolean canRead(Post post, Long viewerId) {

        // 관리자가 숨기거나 삭제한 콘텐츠는 누구에게도 공개하지 않음
        if (post.getModerationStatus() == ModerationStatus.HIDDEN
                || post.getModerationStatus() == ModerationStatus.REMOVED) {
            return false;
        }

        // 작성자는 자신의 분석 대기 콘텐츠 열람 가능
        if (post.getUser().getId().equals(viewerId)) {
            return true;
        }

        // 다른 사용자는 분석 완료 및 공개 승인 필요
        if (post.getAnalysisStatus() != AnalysisStatus.COMPLETED
                || post.getModerationStatus() != ModerationStatus.VISIBLE) {
            return false;
        }

        Integer required = post.getRequiredEpisodeNumber();

        // AI가 안전한 일반 내용으로 판정한 경우
        if (required == null) {
            return true;
        }

        int watched = progressRepository
                .findByUser_IdAndDrama_Id(
                        viewerId,
                        post.getDrama().getId()
                )
                .map(p -> p.getWatchedEpisodeNumber())
                .orElse(0);

        return watched >= required;
    }
}
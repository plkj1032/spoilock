
package com.spoilock.backend.post.service;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.repository.UserProgressRepository;
import com.spoilock.backend.follow.repository.UserFollowRepository;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.entity.PostAudience;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostVisibilityService {

    private final UserProgressRepository progressRepository;

    // 팔로우 관계 확인
    private final UserFollowRepository followRepository;

    // ============================================
    // 게시글 및 댓글 열람 권한 확인
    // ============================================
    public boolean canRead(Post post, Long viewerId) {

        if (post == null || viewerId == null) {
            return false;
        }

        // 1. 관리자 숨김/삭제 콘텐츠 차단
        if (post.getModerationStatus() == ModerationStatus.HIDDEN
                || post.getModerationStatus() == ModerationStatus.REMOVED) {
            return false;
        }

        // 2. 댓글이면 부모 게시글의 열람 권한 먼저 확인
        if (post.getParentPost() != null
                && !canRead(post.getParentPost(), viewerId)) {
            return false;
        }

        // 3. 전체 공개 / 팔로워 공개 권한 확인
        if (!canViewAudience(post, viewerId)) {
            return false;
        }

        // 4. 작성자는 자신의 분석 대기 글 열람 가능
        if (post.getUser().getId().equals(viewerId)) {
            return true;
        }

        // 5. 다른 사용자는 AI 분석 완료 및 공개 승인 필요
        if (post.getAnalysisStatus() != AnalysisStatus.COMPLETED
                || post.getModerationStatus() != ModerationStatus.VISIBLE) {
            return false;
        }

        // 6. 스포일러가 아니라면 공개 가능
        Integer required = post.getRequiredEpisodeNumber();

        if (required == null) {
            return true;
        }

        // 7. 시청 진도 확인
        int watched = progressRepository
                .findByUser_IdAndDrama_Id(
                        viewerId,
                        post.getDrama().getId()
                )
                .map(p -> p.getWatchedEpisodeNumber())
                .orElse(0);

        return watched >= required;
    }

    // ============================================
    // 게시글 공개 범위 검사
    // ============================================
    private boolean canViewAudience(
            Post post,
            Long viewerId
    ) {
        // 댓글은 부모 게시글의 공개 범위를 따름
        Post target = post.getParentPost() != null
                ? post.getParentPost()
                : post;

        Long authorId = target.getUser().getId();

        // 작성자 본인은 항상 허용
        if (authorId.equals(viewerId)) {
            return true;
        }

        // 전체 공개
        if (target.getAudience() == PostAudience.PUBLIC) {
            return true;
        }

        // 팔로워 공개: 조회자가 작성자를 팔로우해야 함
        return followRepository
                .existsByFollower_IdAndFollowing_Id(
                        viewerId,
                        authorId
                );
    }
}

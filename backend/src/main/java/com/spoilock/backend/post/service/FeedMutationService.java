package com.spoilock.backend.post.service;

import com.spoilock.backend.ai.service.PostAnalysisRequested;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.entity.PostAudience;
import com.spoilock.backend.post.repository.PostRepository;
import com.spoilock.backend.user.enums.AccountStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FeedMutationService {

    private final PostRepository postRepository;
    private final PostVisibilityService visibilityService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================
    // 1. 게시글 수정
    // =========================================
    @Transactional
    public void updatePost(
            Long postId,
            Long userId,
            String content,
            PostAudience audience
    ) {
        String value = validateContent(content, 2000);

        if (audience == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "공개 범위를 선택해 주세요."
            );
        }

        Post post = findOwnedRootPost(postId, userId);

        // 본문이 변경되면 Post 내부에서 기존 판정을 초기화한다.
        boolean contentChanged = post.editPost(value, audience);

        // 공개 범위만 변경한 경우 기존 본문 판정은 유지한다.
        if (contentChanged) {
            requestAnalysis(post);
        }
    }

    // =========================================
    // 2. 게시글 삭제
    // =========================================
    @Transactional
    public void deletePost(
            Long postId,
            Long userId
    ) {
        Post post = findOwnedRootPost(postId, userId);

        // 소프트 삭제 + 본문 버전 증가
        post.removeByAuthor();
    }

    // =========================================
    // 3. 댓글 수정
    // =========================================
    @Transactional
    public void updateComment(
            Long postId,
            Long commentId,
            Long userId,
            String content
    ) {
        String value = validateContent(content, 1000);

        Post comment = findOwnedComment(
                postId,
                commentId,
                userId
        );

        boolean contentChanged = comment.editComment(value);

        if (contentChanged) {
            requestAnalysis(comment);
        }
    }

    // =========================================
    // 4. 댓글 삭제
    // =========================================
    @Transactional
    public void deleteComment(
            Long postId,
            Long commentId,
            Long userId
    ) {
        Post comment = findOwnedComment(
                postId,
                commentId,
                userId
        );

        comment.removeByAuthor();
    }

    // =========================================
    // 5. 작성자 확인 + 수정 대상 잠금
    // =========================================
    private Post findOwnedPost(
            Long id,
            Long userId
    ) {
        // 분석 결과 저장과 같은 행 잠금을 사용한다.
        Post post = postRepository.findByIdForUpdate(id)
                .orElseThrow(() -> notFound());

        if (post.getModerationStatus() == ModerationStatus.REMOVED
                || post.getModerationStatus() == ModerationStatus.HIDDEN
                || post.getDrama().getStatus() != DramaStatus.PUBLISHED) {
            throw notFound();
        }

        if (!post.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "작성자만 수정하거나 삭제할 수 있습니다."
            );
        }

        if (post.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "현재 계정으로는 수정하거나 삭제할 수 없습니다."
            );
        }

        return post;
    }

    // =========================================
    // 6. 일반 게시글인지 확인
    // =========================================
    private Post findOwnedRootPost(
            Long postId,
            Long userId
    ) {
        Post post = findOwnedPost(postId, userId);

        if (post.getParentPost() != null) {
            throw notFound();
        }

        return post;
    }

    // =========================================
    // 7. 댓글과 원글의 관계 및 접근 권한 확인
    // =========================================
    private Post findOwnedComment(
            Long postId,
            Long commentId,
            Long userId
    ) {
        Post comment = findOwnedPost(commentId, userId);
        Post parent = comment.getParentPost();

        if (parent == null || !parent.getId().equals(postId)) {
            throw notFound();
        }

        if (!visibilityService.canRead(parent, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 게시글을 현재 열람할 수 없습니다."
            );
        }

        return comment;
    }

    // =========================================
    // 8. 변경된 본문 재분석 요청
    // =========================================
    private void requestAnalysis(Post post) {
        postRepository.flush();

        // 기존 리스너가 트랜잭션 커밋 후 비동기로 분석한다.
        eventPublisher.publishEvent(
                new PostAnalysisRequested(
                        post.getId(),
                        post.getDrama().getId(),
                        post.getContent(),
                        post.getContentRevision()
                )
        );
    }

    // =========================================
    // 9. 본문 검증
    // =========================================
    private String validateContent(
            String content,
            int maxLength
    ) {
        if (content == null
                || content.isBlank()
                || content.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "내용은 1~" + maxLength + "자 사이로 입력해 주세요."
            );
        }

        return content.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "게시글 또는 댓글을 찾을 수 없습니다."
        );
    }
}
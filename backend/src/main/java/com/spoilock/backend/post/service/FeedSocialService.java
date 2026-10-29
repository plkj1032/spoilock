package com.spoilock.backend.post.service;

import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.notification.domain.entity.Notification;
import com.spoilock.backend.notification.repository.NotificationRepository;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.entity.PostLike;
import com.spoilock.backend.post.entity.id.PostLikeId;
import com.spoilock.backend.post.repository.PostLikeRepository;
import com.spoilock.backend.post.repository.PostRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedSocialService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final PostVisibilityService visibilityService;
    private final NotificationRepository notificationRepository;

    private static final List<ModerationStatus> ALLOWED =
            List.of(
                    ModerationStatus.PENDING,
                    ModerationStatus.VISIBLE
            );

    public record SocialSummary(
            long likeCount,
            long commentCount,
            boolean liked
    ) {}

    public record CommentResponse(
            Long id,
            Long authorId,
            String nickname,
            String content,
            boolean visible,
            LocalDateTime createdAt
    ) {}

    private Post findRootPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "피드를 찾을 수 없습니다."
                        )
                );

        if (post.getParentPost() != null
                || !ALLOWED.contains(post.getModerationStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "피드를 찾을 수 없습니다."
            );
        }

        return post;
    }

    private void requireReadable(Post post, Long userId) {
        if (!visibilityService.canRead(post, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 피드를 아직 열람할 수 없습니다."
            );
        }
    }

    // 좋아요 수 / 댓글 수 / 내 좋아요 여부
    @Transactional(readOnly = true)
    public SocialSummary summary(Long postId, Long userId) {
        findRootPost(postId);

        return new SocialSummary(
                likeRepository.countByPost_Id(postId),
                postRepository.countByParentPost_IdAndModerationStatusIn(
                        postId, ALLOWED
                ),
                likeRepository.existsById(
                        new PostLikeId(userId, postId)
                )
        );
    }

    // 좋아요
    // 좋아요: 실제로 새 좋아요가 저장될 때만 알림 생성
    @Transactional
    public SocialSummary like(Long postId, Long userId) {
        Post post = findRootPost(postId);
        requireReadable(post, userId);

        PostLikeId id = new PostLikeId(userId, postId);

        if (!likeRepository.existsById(id)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED
                            )
                    );

            likeRepository.saveAndFlush(
                    PostLike.create(user, post)
            );

            Long authorId = post.getUser().getId();

            if (!authorId.equals(userId)) {
                notificationRepository.save(
                        Notification.createPostLike(
                                authorId,
                                user.getId(),
                                user.getNickname()
                        )
                );
            }
        }

        return summary(postId, userId);
    }

    // 좋아요 취소
    @Transactional
    public SocialSummary unlike(Long postId, Long userId) {
        Post post = findRootPost(postId);
        requireReadable(post, userId);

        likeRepository.deleteById(
                new PostLikeId(userId, postId)
        );

        likeRepository.flush();

        return summary(postId, userId);
    }

    // 댓글 작성
    // 댓글 작성 및 게시글 작성자 알림
    @Transactional
    public CommentResponse comment(
            Long postId,
            Long userId,
            String content
    ) {
        String value = content == null ? "" : content.trim();

        if (value.isBlank() || value.length() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "댓글은 1~1000자 사이여야 합니다."
            );
        }

        Post parent = findRootPost(postId);
        requireReadable(parent, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED
                        )
                );

        Post saved = postRepository.saveAndFlush(
                Post.createComment(user, parent, value)
        );

        Long authorId = parent.getUser().getId();

        if (!authorId.equals(userId)) {
            notificationRepository.save(
                    Notification.createPostComment(
                            authorId,
                            user.getId(),
                            user.getNickname()
                    )
            );
        }

        return toResponse(saved, userId);
    }

    // 최근 댓글 20개 (페이지 조회 지원)
    @Transactional(readOnly = true)
    public Page<CommentResponse> comments(
            Long postId,
            Long userId,
            int page
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page는 0 이상이어야 합니다."
            );
        }

        Post parent = findRootPost(postId);
        requireReadable(parent, userId);

        return postRepository
                .findByParentPost_IdAndModerationStatusIn(
                        postId,
                        ALLOWED,
                        PageRequest.of(
                                page,
                                20,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.desc("id")
                                )
                        )
                )
                .map(comment -> toResponse(comment, userId));
    }

    private CommentResponse toResponse(
            Post comment,
            Long viewerId
    ) {
        boolean visible =
                visibilityService.canRead(comment, viewerId);

        return new CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                visible ? comment.getContent() : null,
                visible,
                comment.getCreatedAt()
        );
    }
}
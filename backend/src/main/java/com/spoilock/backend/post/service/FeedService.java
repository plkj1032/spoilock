
package com.spoilock.backend.post.service;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import com.spoilock.backend.drama.entity.Drama;
import com.spoilock.backend.drama.entity.UserProgress;
import com.spoilock.backend.drama.enums.DramaStatus;
import com.spoilock.backend.drama.repository.DramaRepository;
import com.spoilock.backend.drama.repository.UserProgressRepository;

import com.spoilock.backend.post.dto.CreateFeedRequest;
import com.spoilock.backend.post.dto.FeedItemResponse;
import com.spoilock.backend.post.dto.FeedPageResponse;
import com.spoilock.backend.post.entity.Post;
import com.spoilock.backend.post.repository.PostRepository;
import com.spoilock.backend.post.repository.FeedQueryRepository;

import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final FeedQueryRepository feedQueryRepository;
    private final UserRepository userRepository;
    private final DramaRepository dramaRepository;
    private final UserProgressRepository progressRepository;
    private final PostVisibilityService visibilityService;

    // 커서 정보
    private record FeedCursor(
            LocalDateTime createdAt,
            Long id
    ) {}

    // ============================================
    // 1. 피드 작성
    // ============================================
    @Transactional
    public Long createPost(
            Long userId,
            CreateFeedRequest request
    ) {
        User user = userRepository.findById(userId)
                .filter(u ->
                        u.getAccountStatus() == AccountStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "글을 작성할 수 없는 계정입니다."
                        )
                );

        Drama drama = dramaRepository.findByIdAndStatus(
                request.dramaId(),
                DramaStatus.PUBLISHED
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공개된 드라마를 찾을 수 없습니다."
                )
        );

        Post post = Post.create(
                user,
                drama,
                request.content()
        );

        // 생성 시 PENDING 상태 유지
        return postRepository.saveAndFlush(post).getId();
    }

    // ============================================
    // 2. 전체 / 팔로잉 피드 조회
    // ============================================
    @Transactional(readOnly = true)
    public FeedPageResponse getFeed(
            Long viewerId,
            String type,
            String cursor,
            int size
    ) {
        if (!"ALL".equals(type)
                && !"FOLLOWING".equals(type)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "type은 ALL 또는 FOLLOWING이어야 합니다."
            );
        }

        if (size < 1 || size > 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size는 1~30이어야 합니다."
            );
        }

        FeedCursor parsedCursor = parseCursor(cursor);

        // 한 개 더 조회해서 다음 페이지 존재 여부 확인
        List<Post> result = feedQueryRepository.findFeed(
                viewerId,
                type,
                parsedCursor == null
                        ? null : parsedCursor.createdAt(),
                parsedCursor == null
                        ? null : parsedCursor.id(),
                size + 1
        );

        boolean hasNext = result.size() > size;

        List<Post> posts = hasNext
                ? result.subList(0, size)
                : result;

        // 피드에 등장한 드라마 ID들을 수집
        Set<Long> dramaIds = posts.stream()
                .map(p -> p.getDrama().getId())
                .collect(Collectors.toSet());

        // 로그인 사용자의 시청 진도를 한 번에 조회
        List<UserProgress> progresses = dramaIds.isEmpty()
                ? List.of()
                : progressRepository.findByUser_IdAndDrama_IdIn(
                viewerId,
                dramaIds
        );

        Map<Long, Integer> watchedByDrama = progresses.stream()
                .collect(Collectors.toMap(
                        p -> p.getDrama().getId(),
                        UserProgress::getWatchedEpisodeNumber
                ));

        // 스포일러 보호를 적용한 응답
        List<FeedItemResponse> items = posts.stream()
                .map(p -> toResponse(
                        p,
                        viewerId,
                        watchedByDrama
                ))
                .toList();

        String nextCursor =
                hasNext && !posts.isEmpty()
                        ? encodeCursor(posts.get(posts.size() - 1))
                        : null;

        return new FeedPageResponse(
                items,
                nextCursor,
                hasNext
        );
    }

    // ============================================
    // 3. 스포일러 보호 응답 생성
    // ============================================
    private FeedItemResponse toResponse(
            Post post,
            Long viewerId,
            Map<Long, Integer> watchedByDrama
    ) {
        boolean mine = post.getUser().getId().equals(viewerId);

        // 분석이 완료되고 공개 승인된 게시글만 공개
        boolean approved =
                post.getAnalysisStatus() == AnalysisStatus.COMPLETED
                        && post.getModerationStatus() == ModerationStatus.VISIBLE;

        Integer required = post.getRequiredEpisodeNumber();

        int watched = watchedByDrama.getOrDefault(
                post.getDrama().getId(),
                0
        );

        boolean watchedEnough =
                required == null || watched >= required;

        // 작성자는 자신의 분석 대기 원문을 볼 수 있음
        boolean visible =
                mine || (approved && watchedEnough);

        String blockReason = null;

        if (!visible) {
            if (!approved) {
                blockReason =
                        post.getAnalysisStatus() == AnalysisStatus.FAILED
                                ? "REVIEW"
                                : "ANALYZING";
            } else {
                blockReason = "NOT_WATCHED";
            }
        }

        return new FeedItemResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getNickname(),
                post.getDrama().getId(),
                post.getDrama().getTitle(),

                // 차단된 원문은 서버에서 제거
                visible ? post.getContent() : null,

                visible,
                blockReason,
                post.getAnalysisStatus().name(),
                post.getCreatedAt()
        );
    }

    // ============================================
    // 4. 커서 인코딩
    // ============================================
    private String encodeCursor(Post post) {
        String value =
                post.getCreatedAt() + "|" + post.getId();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        value.getBytes(StandardCharsets.UTF_8)
                );
    }

    // ============================================
    // 5. 커서 디코딩
    // ============================================
    private FeedCursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        if (cursor.length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "잘못된 커서입니다."
            );
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );

            String[] parts = decoded.split("\\|", -1);

            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }

            LocalDateTime createdAt =
                    LocalDateTime.parse(parts[0]);

            Long id = Long.parseLong(parts[1]);

            if (id <= 0) {
                throw new IllegalArgumentException();
            }

            return new FeedCursor(createdAt, id);

        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "유효하지 않은 페이지 커서입니다.",
                    ex
            );
        }
    }

    public record ProfileFeedPreview(
            Long userId,
            long postCount,
            List<FeedItemResponse> recentPosts
    ) {}

    // 사용자별 전체 피드
    @Transactional(readOnly = true)
    public FeedPageResponse getUserFeed(
            Long viewerId,
            Long authorId,
            String cursor,
            int size
    ) {
        if (size < 1 || size > 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size는 1~30이어야 합니다."
            );
        }

        User author = userRepository.findById(authorId)
                .filter(u ->
                        u.getAccountStatus() == AccountStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );

        FeedCursor parsed = parseCursor(cursor);

        List<Post> result = feedQueryRepository.findUserFeed(
                author.getId(),
                parsed == null ? null : parsed.createdAt(),
                parsed == null ? null : parsed.id(),
                size + 1
        );

        boolean hasNext = result.size() > size;

        List<Post> posts = hasNext
                ? result.subList(0, size)
                : result;

        Set<Long> dramaIds = posts.stream()
                .map(p -> p.getDrama().getId())
                .collect(Collectors.toSet());

        List<UserProgress> progress = dramaIds.isEmpty()
                ? List.of()
                : progressRepository.findByUser_IdAndDrama_IdIn(
                viewerId,
                dramaIds
        );

        Map<Long, Integer> watched = progress.stream()
                .collect(Collectors.toMap(
                        p -> p.getDrama().getId(),
                        UserProgress::getWatchedEpisodeNumber
                ));

        List<FeedItemResponse> items = posts.stream()
                .map(p -> toResponse(p, viewerId, watched))
                .toList();

        String nextCursor = hasNext && !posts.isEmpty()
                ? encodeCursor(posts.get(posts.size() - 1))
                : null;

        return new FeedPageResponse(
                items,
                nextCursor,
                hasNext
        );
    }

    // 프로필 요약: 게시글 수 + 최신 피드 3개
    @Transactional(readOnly = true)
    public ProfileFeedPreview getProfilePreview(
            Long viewerId,
            Long authorId
    ) {
        FeedPageResponse page = getUserFeed(
                viewerId,
                authorId,
                null,
                3
        );

        long count =
                postRepository
                        .countByUser_IdAndParentPostIsNullAndModerationStatusIn(
                                authorId,
                                List.of(
                                        ModerationStatus.PENDING,
                                        ModerationStatus.VISIBLE
                                )
                        );

        return new ProfileFeedPreview(
                authorId,
                count,
                page.items()
        );
    }

}

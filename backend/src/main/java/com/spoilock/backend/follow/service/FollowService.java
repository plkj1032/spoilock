package com.spoilock.backend.follow.service;

import com.spoilock.backend.follow.dto.FollowStatsResponse;
import com.spoilock.backend.follow.dto.FollowUserResponse;
import com.spoilock.backend.follow.entity.UserFollow;
import com.spoilock.backend.follow.entity.id.UserFollowId;
import com.spoilock.backend.follow.repository.UserFollowRepository;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;

    // =========================================
    // 1. 사용자 존재 및 계정 상태 확인
    // =========================================
    private User findActiveUser(Long userId)
    {
        return userRepository.findById(userId)
                .filter(user ->
                        user.getAccountStatus() == AccountStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );
    }

    // =========================================
    // 2. 자기 자신 팔로우 방지
    // =========================================
    private void validateNotSelf(
            Long followerId,
            Long followingId
    )
    {
        if(followerId.equals(followingId))
        {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자기 자신을 팔로우할 수 없습니다."
            );
        }
    }

    // =========================================
    // 3. 팔로우
    // =========================================
    @Transactional
    public FollowStatsResponse follow(
            Long followerId,
            Long followingId
    )
    {
        validateNotSelf(followerId, followingId);

        User follower = findActiveUser(followerId);
        User following = findActiveUser(followingId);

        boolean exists = followRepository
                .existsByFollower_IdAndFollowing_Id(
                        followerId,
                        followingId
                );

        if(exists)
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 팔로우한 사용자입니다."
            );
        }

        UserFollow follow = UserFollow.create(
                follower,
                following
        );

        try{
            // DB 복합 PK로 동시 중복 요청도 방어
            followRepository.saveAndFlush(follow);
        }catch(DataIntegrityViolationException ex)
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "팔로우 관계를 등록할 수 없습니다.",
                    ex
            );
        }

        return getStats(followingId, followerId);
    }

    // =========================================
    // 4. 언팔로우
    // =========================================
    @Transactional
    public void unfollow(
            Long followerId,
            Long followingId
    )
    {
        validateNotSelf(followerId,followingId);

        findActiveUser(followerId);
        findActiveUser(followingId);

        UserFollowId id = new UserFollowId(
                followerId,
                followingId
        );

        // 이미 삭제된 관계라면 별도 오류 없이 처리
        followRepository.findById(id)
                .ifPresent(followRepository::delete);
    }

    // =========================================
    // 5. 팔로우 통계
    // =========================================
    @Transactional(readOnly = true)
    public FollowStatsResponse getStats(
            Long userId,
            Long viewerId
    )
    {
        findActiveUser(userId);

        long followerCount = followRepository.countByFollowing_Id(userId);

        long followingCount = followRepository.countByFollower_id(userId);

        boolean isFollowing = !userId.equals(viewerId)
                && followRepository
                .existsByFollower_IdAndFollowing_Id(
                        viewerId,
                        userId
                );

        return new FollowStatsResponse(
                userId,
                followerCount,
                followingCount,
                isFollowing
        );
    }

    // =========================================
    // 6. 팔로워 목록
    // =========================================
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getFollowers(
            Long userId,
            int page,
            int size
    )
    {
        findActiveUser(userId);

        Pageable pageable = createPageRequest(
                page,
                size
        );

        return followRepository
                .findByFollowing_Id(userId, pageable)
                .map(follow ->
                        new FollowUserResponse(
                                follow.getFollower().getId(),
                                follow.getFollower().getNickname(),
                                follow.getCreatedAt()
                        )
                );
    }

    // =========================================
    // 7. 팔로잉 목록
    // =========================================
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getFollowing(
            Long userId,
            int page,
            int size
    )
    {
        findActiveUser(userId);

        PageRequest pageable = createPageRequest(
                page,
                size
        );

        return followRepository
                .findByFollower_Id(userId, pageable)
                .map(follow ->
                        new FollowUserResponse(
                                follow.getFollowing().getId(),
                                follow.getFollowing().getNickname(),
                                follow.getCreatedAt()
                        )
                );
    }

    // =========================================
    // 8. 페이지 요청 검증
    // =========================================
    private PageRequest createPageRequest(
            int page,
            int size
    )
    {
        if(page < 0 || size < 1 || size > 50)
        {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page는 0이상, size는 1~50이어야 합니다."
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

}

package com.spoilock.backend.user.service;

import com.spoilock.backend.common.dto.PageResponse;
import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.dto.MyPageResponse;
import com.spoilock.backend.user.dto.UpdateProfileRequest;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.MyPageQueryRepository;
import com.spoilock.backend.user.repository.MyRoomPageQueryRepository;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final MyPageQueryRepository myPageQueryRepository;
    private final MyRoomPageQueryRepository myRoomPageQueryRepository;

    public MyPageResponse getMyPage(
            Long userId,
            boolean includeRooms
    ) {
        User user = findActiveUser(userId);

        return new MyPageResponse(
                toProfile(user),
                myPageQueryRepository.findWatchingDramas(userId),
                includeRooms
                        ? myPageQueryRepository.findJoinedRooms(userId)
                        : List.of()
        );
    }

    public PageResponse<MyPageResponse.JoinedRoom> getMyRoomPage(
            Long userId,
            int page,
            int size
    ) {
        findActiveUser(userId);

        return myRoomPageQueryRepository.findPage(
                userId,
                page,
                size
        );
    }

    @Transactional
    public MeResponse updateProfile(
            Long userId,
            UpdateProfileRequest request
    ) {
        User user = findActiveUser(userId);

        user.updateNickname(request.nickname());

        return toProfile(user);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "현재 이용할 수 없는 계정입니다."
            );
        }

        return user;
    }

    private MeResponse toProfile(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
    }
}
package com.spoilock.backend.user.service;

import com.spoilock.backend.user.dto.LoginRequest;
import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.dto.SignUpRequest;
import com.spoilock.backend.user.dto.SignUpResponse;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request)
    {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmail(email))
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocal(
                email,
                encodedPassword,
                request.nickname().trim()
        );

        User savedUser = userRepository.save(user);

        return new SignUpResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getNickname()
        );
    }

    // 로그인
    @Transactional
    public LoginResult login(LoginRequest request)
    {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(this::loginFailed);

        if(user.getPasswordHash() == null
            || !passwordEncoder.matches(
                    request.password(),
                    user.getPasswordHash()
        )
            || user.getAccountStatus()
                != AccountStatus.ACTIVE) {
            throw loginFailed();
        }

        String accessToken = jwtService.createAccessToken(user);

        String refreshToken = refreshTokenService.issue(user);

        return new LoginResult(
                accessToken,
                refreshToken,
                toMeResponse(user)
        );
    }

    // 현재 로그인 사용자 조회
    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED
                )
        );

        return toMeResponse(user);
    }

    private MeResponse toMeResponse(User user)
    {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
    }

    private ResponseStatusException loginFailed()
    {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "이메일 또는 비밀번호가 올바르지 않습니다."
        );
    }

    public record LoginResult(
            String accessToken,
            String refreshToken,
            MeResponse user
    ){}

    // Refresh Token으로 토큰 재발급
    @Transactional
    public LoginResult refresh(String rawRefreshToken)
    {
        RefreshTokenService.RotatedToken rotated =
                refreshTokenService.rotate(rawRefreshToken);

        User user = rotated.user();

        String accessToken = jwtService.createAccessToken(user);

        return new LoginResult(
                accessToken,
                rotated.refreshToken(),
                toMeResponse(user)
        );
    }

    // 전 기기 로그아웃
    @Transactional
    public void logout(String rawRefreshToken)
    {
        refreshTokenService.logoutAll(rawRefreshToken);
    }

}

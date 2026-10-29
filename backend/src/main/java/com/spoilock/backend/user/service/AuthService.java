
package com.spoilock.backend.user.service;

import com.spoilock.backend.user.dto.LoginRequest;
import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.dto.SignUpRequest;
import com.spoilock.backend.user.dto.SignUpResponse;

import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TermsService termsService;

    // 신규: 이메일 인증 서비스
    private final EmailVerificationService emailVerificationService;

    // ==============================================
    // 1. 회원가입
    // ==============================================
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        String email = emailVerificationService
                .normalizeEmail(request.email());

        String nickname = normalizeNickname(request.nickname());

        // 이메일 중복
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

        // 닉네임 중복
        if (userRepository.existsByNicknameKey(
                nickname.toLowerCase(Locale.ROOT)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 닉네임입니다."
            );
        }

        // BCrypt는 72바이트 입력 한계가 있으므로
        // UTF-8 기준으로도 검사
        if (request.password()
                .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."
            );
        }

        // 이메일 인증 증명 토큰 검사 및 일회성 사용
        // 회원 생성과 같은 트랜잭션에서 처리
        emailVerificationService.consumeSignupProof(
                email,
                request.signupProof()
        );

        String encodedPassword =
                passwordEncoder.encode(request.password());

        User user = User.createLocal(
                email,
                encodedPassword,
                nickname
        );

        try {
            // DB UNIQUE 제약으로 동시 가입 충돌도 차단
            User saved = userRepository.saveAndFlush(user);

            // 필수 약관 검증 및 동의 이력 저장
            termsService.recordSignupAcceptances(
                    saved,
                    request.acceptedTermsIds()
            );

            return new SignUpResponse(
                    saved.getId(),
                    saved.getEmail(),
                    saved.getNickname()
            );
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이메일 또는 닉네임이 이미 사용 중입니다.",
                    ex
            );
        }
    }

    // ==============================================
    // 2. 닉네임 사용 가능 여부
    // ==============================================
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {

        String normalized = normalizeNickname(nickname);

        return !userRepository.existsByNicknameKey(
                normalized.toLowerCase(Locale.ROOT)
        );
    }

    private String normalizeNickname(String nickname) {

        if (nickname == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "닉네임을 입력해주세요."
            );
        }

        String result = nickname.trim();

        if (result.length() < 2 || result.length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "닉네임은 2~20자로 입력해주세요."
            );
        }

        return result;
    }

    // ==============================================
    // 3. 기존 로그인
    // ==============================================
    @Transactional
    public LoginResult login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(this::loginFailed);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )
                || user.getAccountStatus()
                != AccountStatus.ACTIVE) {
            throw loginFailed();
        }

        // 기존 로그인 무효화
        User currentUser = refreshTokenService.beginNewLogin(user);

        // 새 로그인 버전으로 JWT 발급
        String accessToken = jwtService.createAccessToken(currentUser);

        // 새로운 Refresh Token 발급
        String refreshToken = refreshTokenService.issue(currentUser);

        return new LoginResult(
                accessToken,
                refreshToken,
                toMeResponse(currentUser)
        );
    }

    // ==============================================
    // 4. 로그인 사용자 조회
    // ==============================================
    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED
                        )
                );

        return toMeResponse(user);
    }

    private MeResponse toMeResponse(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
    }

    private ResponseStatusException loginFailed() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "이메일 또는 비밀번호가 올바르지 않습니다."
        );
    }

    public record LoginResult(
            String accessToken,
            String refreshToken,
            MeResponse user
    ) {}

    // ==============================================
    // 5. Refresh Token 갱신
    // ==============================================
    @Transactional
    public LoginResult refresh(String rawRefreshToken) {

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

    // ==============================================
    // 6. 로그아웃
    // ==============================================
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.logoutAll(rawRefreshToken);
    }
}

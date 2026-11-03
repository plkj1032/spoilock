
package com.spoilock.backend.user.service;

import com.spoilock.backend.user.dto.MeResponse;
import com.spoilock.backend.user.entity.OAuthAccount;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.enums.OAuthProvider;
import com.spoilock.backend.user.repository.OAuthAccountRepository;
import com.spoilock.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TermsService termsService;
    private final NicknamePolicy nicknamePolicy;

    public record OAuthResult(
            AuthService.LoginResult loginResult,
            String pendingKakaoId
    ) {}

    public record PendingInfo(
            boolean existing,
            String nickname
    ) {}

    // ==========================================
    // 1. 카카오 OAuth 인증 성공
    // ==========================================
    @Transactional
    public OAuthResult login(OAuth2User kakaoUser) {

        Object rawId = kakaoUser.getAttribute("id");

        if (!(rawId instanceof Number id)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "카카오 사용자 정보를 확인할 수 없습니다."
            );
        }

        String kakaoId = id.toString();

        Optional<OAuthAccount> account =
                findKakaoAccount(kakaoId);

        // 신규 사용자: 회원을 만들지 않고 동의 화면으로
        if (account.isEmpty()) {
            return new OAuthResult(null, kakaoId);
        }

        User user = account.get().getUser();

        requireActive(user);

        // 기존 회원이라도 현재 필수 약관 미동의면 보류
        if (!termsService.hasAcceptedRequiredTerms(user)) {
            return new OAuthResult(null, kakaoId);
        }

        // 약관 동의가 확인된 기존 회원만 로그인
        return new OAuthResult(issueLogin(user), null);
    }

    // ==========================================
    // 2. 추가 가입 화면 상태 조회
    // ==========================================
    @Transactional(readOnly = true)
    public PendingInfo pendingInfo(String kakaoId) {

        Optional<OAuthAccount> account =
                findKakaoAccount(kakaoId);

        if (account.isPresent()) {
            User user = account.get().getUser();
            requireActive(user);

            return new PendingInfo(
                    true,
                    user.getNickname()
            );
        }

        return new PendingInfo(false, null);
    }

    // ==========================================
    // 3. 카카오 회원가입 / 약관 동의 완료
    // ==========================================
    @Transactional
    public AuthService.LoginResult complete(
            String kakaoId,
            String nickname,
            List<Long> acceptedTermsIds
    ) {

        Optional<OAuthAccount> account =
                findKakaoAccount(kakaoId);

        User user;

        if (account.isPresent()) {

            // 기존 회원: 계정 유지, 약관만 추가 동의
            user = account.get().getUser();
            requireActive(user);

        } else {

            // 신규 회원: 닉네임 정책 통과 후 생성
            String cleanNickname =
                    nicknamePolicy.normalize(nickname);

            if (userRepository.existsByNicknameKey(
                    cleanNickname.toLowerCase(Locale.ROOT)
            )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 사용 중인 닉네임입니다."
                );
            }

            user = userRepository.saveAndFlush(
                    User.createKakao(cleanNickname)
            );

            oAuthAccountRepository.saveAndFlush(
                    OAuthAccount.linkKakao(user, kakaoId)
            );
        }

        // 기존 약관 테이블에 KAKAO 동의 이력 저장
        termsService.recordKakaoAcceptances(
                user,
                acceptedTermsIds
        );

        if (!termsService.hasAcceptedRequiredTerms(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "필수 약관 동의가 필요합니다."
            );
        }

        // 모든 검증 완료 후에만 JWT 발급
        return issueLogin(user);
    }

    // ==========================================
    // 4. 공통 로그인 토큰 발급
    // ==========================================
    private AuthService.LoginResult issueLogin(User user) {

        User currentUser =
                refreshTokenService.beginNewLogin(user);

        String accessToken =
                jwtService.createAccessToken(currentUser);

        String refreshToken =
                refreshTokenService.issue(currentUser);

        return new AuthService.LoginResult(
                accessToken,
                refreshToken,
                new MeResponse(
                        currentUser.getId(),
                        currentUser.getEmail(),
                        currentUser.getNickname(),
                        currentUser.getRole().name()
                )
        );
    }

    private Optional<OAuthAccount> findKakaoAccount(
            String kakaoId
    ) {
        return oAuthAccountRepository
                .findByProviderAndProviderUserId(
                        OAuthProvider.KAKAO,
                        kakaoId
                );
    }

    private void requireActive(User user) {
        if (user.getAccountStatus()
                != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "사용할 수 없는 계정입니다."
            );
        }
    }
}

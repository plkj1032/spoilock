package com.spoilock.backend.user.service;

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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthService.LoginResult login(OAuth2User kakaoUser)
    {
        // 1. 카카오 고유 ID 확인
        Object rawId = kakaoUser.getAttribute("id");

        if(!(rawId instanceof  Number id))
        {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "카카오 사용자 정보를 확인할 수 없습니다."
            );
        }

        String kakaoId = id.toString();

        // 2. 기존 카카오 회원 조회
        User user = oAuthAccountRepository
                .findByProviderAndProviderUserId(
                        OAuthProvider.KAKAO,
                        kakaoId
                )
                .map(OAuthAccount::getUser)
                .orElseGet(() -> registerKakaoUser(
                        kakaoId,
                        extractNickname(kakaoUser)
                ));

        // 3. 계정 상태 확인
        if(user.getAccountStatus() != AccountStatus.ACTIVE)
        {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "사용할 수 없는 계정입니다."
            );
        }

        // 4. SPOILOCK 자체 토큰 발급
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);

        return new AuthService.LoginResult(
                accessToken,
                refreshToken,
                new com.spoilock.backend.user.dto.MeResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getNickname(),
                        user.getRole().name()
                )
        );
    }

    // 최초 카카오 로그인 회원 생성
    private User registerKakaoUser(
            String kakaoId,
            String nickname
    )
    {
        User user = userRepository.save(
                User.createKakao(nickname)
        );

        oAuthAccountRepository.save(
                OAuthAccount.linkKakao(user, kakaoId)
        );

        return user;
    }

    // 동의한 카카오 프로필에서 닉네임 추출
    private String extractNickname(OAuth2User kakaoUser)
    {
        Object accountObject = kakaoUser.getAttribute("kakao_account");

        if(accountObject instanceof Map<?,?> account)
        {
            Object profileObject = account.get("profile");

            if(profileObject instanceof Map<?, ?> profile)
            {
                Object nicknameObject = profile.get("nickname");

                if(nicknameObject instanceof String nickname
                        && !nickname.isBlank())
                {
                    String clean = nickname.trim();

                    return clean.substring(
                            0,
                            Math.min(clean.length(), 20)
                    );
                }
            }
        }
        return "카카오회원";
    }

}

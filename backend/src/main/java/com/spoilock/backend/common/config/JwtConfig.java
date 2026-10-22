
package com.spoilock.backend.common.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.spoilock.backend.user.entity.User;
import com.spoilock.backend.user.enums.AccountStatus;
import com.spoilock.backend.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    // JWT 서명 및 검증에 사용할 비밀키
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.jwt.secret-base64}") String encoded
    ) {
        byte[] bytes = Base64.getDecoder().decode(encoded);

        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT Secret Key는 최소 32바이트여야 합니다."
            );
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // JWT 발급
    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(key)
        );
    }

    // JWT 검증
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey key,
            UserRepository userRepository,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> userValidator = jwt -> {

            Long userId;

            // 1. JWT에 담긴 사용자 ID 검증
            try {
                userId = Long.valueOf(jwt.getSubject());
            } catch (NumberFormatException e) {
                return invalidToken();
            }

            // 2. 사용자 조회
            User user = userRepository.findById(userId)
                    .orElse(null);

            if (user == null) {
                return invalidToken();
            }

            // 3. JWT의 Claim 조회
            Object version = jwt.getClaim("ver");
            String role = jwt.getClaimAsString("role");

            // 4. 사용자 상태 및 JWT 검증
            boolean valid =
                    user.getAccountStatus() == AccountStatus.ACTIVE
                            && version instanceof Number
                            && user.getTokenVersion() != null
                            && user.getTokenVersion().equals(
                            ((Number) version).longValue()
                    )
                            && user.getRole().name().equals(role);

            if (!valid) {
                return invalidToken();
            }

            return OAuth2TokenValidatorResult.success();
        };

        // 5. 만료시간, issuer, 사용자 상태 검증
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        userValidator
                )
        );

        return decoder;
    }

    private OAuth2TokenValidatorResult invalidToken() {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(
                        "invalid_token",
                        "유효하지 않은 JWT입니다.",
                        null
                )
        );
    }
}

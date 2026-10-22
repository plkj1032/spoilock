package com.spoilock.backend.user.service;

import com.spoilock.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.access-minutes}")
    private long accessMinutes;

    public String createAccessToken(User user)
    {
        Instant now = Instant.now();

        // JWT Payload
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(
                        now.plus(accessMinutes, ChronoUnit.MINUTES)
                )
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .build();

        // JWT Header - HS256 서명 알고리즘
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

}

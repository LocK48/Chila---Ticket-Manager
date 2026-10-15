package com.chila.backend.auth;

import java.time.Instant;
import java.util.UUID;

import com.chila.backend.user.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    static final long ACCESS_TOKEN_SECONDS = 15 * 60;

    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${chila.auth.jwt-issuer}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    public String createAccessToken(UserEntity user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_SECONDS))
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public UUID userId(String subject) {
        return UUID.fromString(subject);
    }
}

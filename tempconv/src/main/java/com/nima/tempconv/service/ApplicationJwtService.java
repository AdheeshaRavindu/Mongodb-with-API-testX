package com.nima.tempconv.service;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.nima.tempconv.config.JwtConfig;
import com.nima.tempconv.model.User;

@Service
public class ApplicationJwtService {

    private final JwtEncoder jwtEncoder;
    private final java.time.Duration jwtExpiration;

    public ApplicationJwtService(JwtEncoder jwtEncoder, java.time.Duration jwtExpiration) {
        this.jwtEncoder = jwtEncoder;
        this.jwtExpiration = jwtExpiration;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.JWT_ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(jwtExpiration))
                .subject(user.getGoogleId())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("picture", user.getPicture())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}

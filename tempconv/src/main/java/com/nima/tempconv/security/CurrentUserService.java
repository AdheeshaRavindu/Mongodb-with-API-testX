package com.nima.tempconv.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.nima.tempconv.dto.UserResponse;

@Service
public class CurrentUserService {

    public Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    public String requireUserId() {
        return getCurrentJwt()
                .map(Jwt::getSubject)
                .orElseThrow(() -> new IllegalStateException("Authenticated user required"));
    }

    public UserResponse getCurrentUser() {
        Jwt jwt = getCurrentJwt()
                .orElseThrow(() -> new IllegalStateException("Authenticated user required"));
        return new UserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture"));
    }
}

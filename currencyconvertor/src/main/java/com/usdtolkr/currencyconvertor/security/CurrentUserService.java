package com.usdtolkr.currencyconvertor.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

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
}

package com.nima.tempconv.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nima.tempconv.dto.AuthResponse;
import com.nima.tempconv.dto.UserResponse;
import com.nima.tempconv.model.User;
import com.nima.tempconv.repository.UserRepository;
import com.nima.tempconv.service.GoogleTokenVerifierService.GoogleUserProfile;

@Service
public class AuthService {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserRepository userRepository;
    private final ApplicationJwtService applicationJwtService;

    public AuthService(
            GoogleTokenVerifierService googleTokenVerifierService,
            UserRepository userRepository,
            ApplicationJwtService applicationJwtService) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.userRepository = userRepository;
        this.applicationJwtService = applicationJwtService;
    }

    public AuthResponse authenticateWithGoogle(String idToken) {
        GoogleUserProfile profile = googleTokenVerifierService.verify(idToken);
        User user = userRepository.findByGoogleId(profile.googleId())
                .map(existing -> updateUser(existing, profile))
                .orElseGet(() -> createUser(profile));

        String token = applicationJwtService.generateToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }

    private User createUser(GoogleUserProfile profile) {
        Instant now = Instant.now();
        User user = new User();
        user.setGoogleId(profile.googleId());
        user.setEmail(profile.email());
        user.setName(profile.name());
        user.setPicture(profile.picture());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private User updateUser(User user, GoogleUserProfile profile) {
        boolean changed = false;

        if (StringUtils.hasText(profile.email()) && !profile.email().equals(user.getEmail())) {
            user.setEmail(profile.email());
            changed = true;
        }
        if (StringUtils.hasText(profile.name()) && !profile.name().equals(user.getName())) {
            user.setName(profile.name());
            changed = true;
        }
        if (profile.picture() != null && !profile.picture().equals(user.getPicture())) {
            user.setPicture(profile.picture());
            changed = true;
        }

        if (changed) {
            user.setUpdatedAt(Instant.now());
            return userRepository.save(user);
        }

        return user;
    }
}

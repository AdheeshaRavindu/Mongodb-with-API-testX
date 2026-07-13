package com.nima.tempconv.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nima.tempconv.dto.AuthResponse;
import com.nima.tempconv.dto.GoogleAuthRequest;
import com.nima.tempconv.dto.UserResponse;
import com.nima.tempconv.service.AuthService;
import com.nima.tempconv.security.CurrentUserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.authenticateWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(currentUserService.getCurrentUser());
    }
}

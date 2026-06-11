package com.bank.api.controller;

import com.bank.api.dto.request.LoginRequest;
import com.bank.api.dto.request.RegisterRequest;
import com.bank.api.dto.response.AuthResponse;
import com.bank.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /v1/users
     * Spec-compliant registration endpoint.
     * Public — no authentication required.
     */
    @PostMapping("/v1/users")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /v1/auth/login
     * Authentication endpoint — returns JWT.
     * Public — no authentication required.
     */
    @PostMapping("/v1/auth/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
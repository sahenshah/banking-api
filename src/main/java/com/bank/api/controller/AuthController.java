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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles authentication — registration and login.
 * These endpoints are PUBLIC (no JWT required) as configured in SecurityConfig.
 *
 * DESIGN DECISIONS:
 *
 * 1. Controllers are THIN — no business logic here.
 *    Controllers do exactly three things:
 *    a) Accept and validate the HTTP request
 *    b) Call the service
 *    c) Return the HTTP response
 *    If you find yourself writing if/else or try/catch in a controller,
 *    that logic belongs in the service or exception handler.
 *
 * 2. @Valid on @RequestBody — triggers Bean Validation.
 *    If validation fails, Spring throws MethodArgumentNotValidException
 *    which our GlobalExceptionHandler catches and returns 400.
 *    The controller never sees invalid data.
 *
 * 3. register() returns 201 CREATED, login() returns 200 OK.
 *    HTTP semantics: 201 = a new resource was created.
 *    200 = successful operation, no new resource created.
 *    This distinction matters — it's what makes REST "RESTful".
 *
 * 4. @RequestMapping("/auth") — matches SecurityConfig's permitAll("/auth/**")
 *    and JwtAuthFilter's shouldNotFilter("/auth/").
 *    All three must be consistent.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/v1/auth/register
     * Creates a new user account and returns a JWT.
     * Public endpoint — no authentication required.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/auth/login
     * Authenticates credentials and returns a JWT.
     * Public endpoint — no authentication required.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

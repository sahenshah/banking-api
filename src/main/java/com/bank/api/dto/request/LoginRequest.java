package com.bank.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /auth/login.
 *
 * DESIGN DECISION: Separate LoginRequest from RegisterRequest.
 * WHY: Login only needs email + password. Reusing RegisterRequest
 * would require making firstName/lastName optional, which muddies
 * the intent of each DTO. One DTO per use case is cleaner.
 *
 * SECURITY NOTE: Failed login attempts should return the same error
 * message regardless of whether the email exists or the password is wrong.
 * "Invalid credentials" — never "Email not found" or "Wrong password".
 * Distinguishing them allows user enumeration attacks.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password

) {}

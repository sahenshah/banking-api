package com.bank.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /auth/register.
 *
 * DESIGN DECISIONS:
 *
 * 1. DTO separate from entity — never accept a JPA entity as a request body.
 *    WHY: Accepting entities directly allows clients to set fields they
 *    shouldn't (id, createdAt, enabled, password as plaintext into DB).
 *    This is a mass assignment vulnerability. DTOs are an explicit allow-list
 *    of what the client is permitted to provide.
 *
 * 2. Bean Validation annotations (@NotBlank, @Email, @Size).
 *    Validated automatically by Spring when @Valid is on the controller method.
 *    Validation failures produce 400 Bad Request before business logic runs.
 *
 * 3. Java Record — immutable by design.
 *    Records are perfect for DTOs: they're immutable, have auto-generated
 *    equals/hashCode/toString, and require no boilerplate.
 *    DECISION: Use Records for DTOs (immutable request/response objects)
 *    but NOT for entities (JPA requires mutable state + no-args constructor).
 *
 * 4. @Size(min=8) on password — minimum length enforced at API boundary.
 *    PRODUCTION NOTE: A real bank enforces password complexity rules:
 *    uppercase, lowercase, digit, special character. Consider a custom
 *    @ValidPassword annotation backed by a ConstraintValidator.
 */
public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        // SECURITY: Never log this field. Never return it in any response.
        // The controller passes this to the service which hashes it immediately.
        String password

) {}

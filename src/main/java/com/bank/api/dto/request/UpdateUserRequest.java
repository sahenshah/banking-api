package com.bank.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request body for PATCH /v1/users/{userId}
 *
 * DECISION: All fields optional for PATCH.
 * PATCH means partial update — only provided fields are updated.
 * PUT would require all fields. PATCH is correct here per spec.
 */
public record UpdateUserRequest(

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email

) {}
package com.bank.api.dto.response;

import com.bank.api.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for user endpoints.
 *
 * DESIGN DECISIONS:
 *
 * 1. Never expose the User entity directly from a controller.
 *    WHY: The entity contains the hashed password, Hibernate proxy objects,
 *    internal version fields, and lazy-loaded collections that would either
 *    leak sensitive data or cause serialization errors.
 *
 * 2. Static factory method fromUser() on the DTO.
 *    WHY: Keeps the mapping logic in one place — the DTO itself knows
 *    how to construct itself from a domain object. The service just calls
 *    UserResponse.fromUser(user) without knowing the mapping details.
 *    ALTERNATIVE: A dedicated mapper class (MapStruct). Better for large
 *    projects with many mappings. For this exercise, static factory is sufficient.
 *
 * 3. No password field — obviously.
 *    This seems trivial but is worth stating explicitly in an interview:
 *    "The DTO is an explicit allow-list. Password is not on it."
 *
 * 4. Returns UUID id — the client needs this to construct resource URLs
 *    and correlate responses.
 */
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        LocalDateTime createdAt
) {
    /**
     * Maps a User domain entity to a UserResponse DTO.
     * This is the only place this mapping is defined.
     */
    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}

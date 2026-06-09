package com.bank.api.service;

import com.bank.api.domain.User;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles user profile operations.
 *
 * DESIGN DECISIONS:
 *
 * 1. getCurrentUser() takes a UUID, not a User entity.
 *    WHY: The controller extracts the UUID from the SecurityContext and
 *    passes it to the service. The service then loads from DB.
 *    This keeps the controller thin and makes the service method
 *    independently testable — pass any UUID, no Spring Security context needed.
 *
 * 2. Returns UserResponse DTO, not User entity.
 *    WHY: Services should return data in the shape consumers need.
 *    The controller shouldn't need to know how to map entities to DTOs.
 *    ALTERNATIVE: Return the entity and map in the controller.
 *    We prefer mapping in the service — it's one less responsibility
 *    for the controller and easier to test the mapping logic.
 *
 * 3. @Transactional(readOnly=true) on reads.
 *    WHY: Signals to Hibernate and the DB that this transaction won't
 *    modify data. Allows DB-level optimisations (read replicas, skip
 *    dirty checking). Always use readOnly=true for query-only methods.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param userId UUID from the JWT SecurityContext
     * @return user profile DTO
     * @throws ResourceNotFoundException if user no longer exists
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.fromUser(user);
    }
}

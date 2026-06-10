package com.bank.api.service;

import com.bank.api.domain.User;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.exception.AccessDeniedException;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns a user profile by ID.
     * Throws 404 if user does not exist.
     * Throws 403 if the requesting user is not the owner.
     *
     * DECISION: Check existence first, then ownership.
     * WHY: The spec has separate scenarios for 404 (not found)
     * and 403 (found but forbidden). We must distinguish them.
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId, UUID authenticatedUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        return UserResponse.fromUser(user);
    }
}
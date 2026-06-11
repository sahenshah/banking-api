package com.bank.api.service;

import com.bank.api.domain.User;
import com.bank.api.dto.request.UpdateUserRequest;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.exception.AccessDeniedException;
import com.bank.api.exception.ConflictException;
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

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId, UUID authenticatedUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        return UserResponse.fromUser(user);
    }

    /**
     * Partially updates a user's profile.
     * Only updates fields that are provided (not null).
     *
     * DECISION: Null check before updating each field.
     * WHY: PATCH semantics — only update what was provided.
     * If firstName is null in the request, keep the existing value.
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UUID authenticatedUserId,
                                   UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(request.email());
        }

        userRepository.save(user);
        return UserResponse.fromUser(user);
    }

    /**
     * Deletes a user.
     *
     * DECISION: Cannot delete if user has accounts.
     * WHY: The spec explicitly states this — returning 409 Conflict
     * if the user has bank accounts. This prevents orphaned accounts.
     */
    @Transactional
    public void deleteUser(UUID userId, UUID authenticatedUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (!user.getAccounts().isEmpty()) {
            throw new ConflictException(
                    "Cannot delete user with active bank accounts. " +
                            "Please close all accounts first."
            );
        }

        userRepository.delete(user);
    }
}
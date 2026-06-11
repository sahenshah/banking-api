package com.bank.api.controller;

import com.bank.api.dto.request.UpdateUserRequest;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /v1/users/{userId}
     * Returns 403 if userId belongs to another user.
     * Returns 404 if userId does not exist.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID authenticatedUserId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(userService.getUser(userId, authenticatedUserId));
    }

    /**
     * PATCH /v1/users/{userId}
     * Partially updates user profile.
     * Returns 403 if userId belongs to another user.
     * Returns 404 if userId does not exist.
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID authenticatedUserId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(
                userService.updateUser(userId, authenticatedUserId, request)
        );
    }

    /**
     * DELETE /v1/users/{userId}
     * Deletes a user — only if they have no bank accounts.
     * Returns 403 if userId belongs to another user.
     * Returns 404 if userId does not exist.
     * Returns 409 if user has active bank accounts.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID authenticatedUserId = UUID.fromString(userDetails.getUsername());
        userService.deleteUser(userId, authenticatedUserId);
        return ResponseEntity.noContent().build();
    }
}
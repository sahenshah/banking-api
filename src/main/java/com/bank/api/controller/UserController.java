package com.bank.api.controller;

import com.bank.api.dto.response.UserResponse;
import com.bank.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * Returns the profile of the requested user.
     * Returns 403 if userId belongs to another user.
     * Returns 404 if userId does not exist.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID authenticatedUserId = UUID.fromString(userDetails.getUsername());
        UserResponse response = userService.getUser(userId, authenticatedUserId);
        return ResponseEntity.ok(response);
    }
}
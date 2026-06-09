package com.bank.api.controller;

import com.bank.api.dto.response.UserResponse;
import com.bank.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Handles user profile operations.
 * All endpoints require authentication (JWT Bearer token).
 *
 * DESIGN DECISIONS:
 *
 * 1. /users/me pattern — not /users/{id}.
 *    WHY: Users should only ever see their own profile. Exposing /users/{id}
 *    creates an IDOR (Insecure Direct Object Reference) vulnerability —
 *    a user could try /users/someone-elses-id. With /users/me, the identity
 *    always comes from the JWT — the client cannot request another user's data.
 *    INTERVIEW TALKING POINT: "I eliminated an entire class of authorization
 *    bugs by using /me instead of path parameters for the user's own resource."
 *
 * 2. @AuthenticationPrincipal UserDetails.
 *    Spring injects the authenticated principal directly into the method.
 *    We extract the UUID from userDetails.getUsername() — recall that in
 *    UserDetailsServiceImpl we set username = UUID string.
 *    This avoids manually parsing the SecurityContext in every controller method.
 *
 * 3. UUID extracted in the controller, not the service.
 *    WHY: The controller owns the HTTP/security boundary. Extracting identity
 *    from the security context is a boundary concern. The service receives
 *    a plain UUID — it doesn't need to know about Spring Security at all.
 *    This makes the service independently testable.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/users/me
     * Returns the profile of the currently authenticated user.
     * Requires: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }
}

package com.bank.api.dto.response;

/**
 * Response body for POST /auth/login and POST /auth/register.
 *
 * DESIGN DECISIONS:
 *
 * 1. Returns token on registration as well as login.
 *    WHY: Better UX — user registers and is immediately authenticated.
 *    They don't need to make a second login request.
 *    This is a product decision, not a security one — both approaches are valid.
 *
 * 2. Includes tokenType = "Bearer".
 *    WHY: Tells the client exactly how to use the token.
 *    The Authorization header format is: "Bearer <token>"
 *    Making this explicit in the response prevents client-side guessing.
 *
 * 3. Includes expiresIn (seconds).
 *    WHY: Allows the client to proactively refresh before expiry
 *    rather than waiting for a 401. Good API design tells clients
 *    everything they need to use the token correctly.
 *
 * 4. Does NOT return user details here — client calls GET /users/me
 *    for that. Single responsibility per endpoint.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn
) {
    // Convenience factory method — always Bearer type
    public static AuthResponse of(String token, long expiresInMs) {
        return new AuthResponse(token, "Bearer", expiresInMs / 1000);
    }
}

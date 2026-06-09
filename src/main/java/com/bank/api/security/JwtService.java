package com.bank.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Handles all JWT operations: generation, validation, and claim extraction.
 *
 * <p>DESIGN DECISIONS:
 *
 * <p>1. @Service — Spring manages this as a singleton bean. JWT operations
 *    are stateless so a singleton is safe and efficient.
 *
 * <p>2. HS256 signing algorithm — HMAC-SHA256. Uses a shared secret key.
 *    ALTERNATIVE: RS256 (RSA asymmetric). Private key signs, public key verifies.
 *    RS256 is better for microservices — each service only needs the public key
 *    to verify tokens, never the private key. For a monolith like this exercise,
 *    HS256 is appropriate and simpler.
 *    INTERVIEW TALKING POINT: "In a distributed Barclays system I'd use RS256
 *    with a JWKS endpoint so downstream services can verify tokens without
 *    sharing the signing secret."
 *
 * <p>3. Subject = UUID string, not email.
 *    Email can change (user updates profile). UUID is immutable.
 *    Using email as subject means a token issued before an email change
 *    would still work with the old email — a security bug.
 *
 * <p>4. We do NOT store tokens server-side.
 *    CONSEQUENCE: Logout cannot truly invalidate a token before expiry.
 *    PRODUCTION SOLUTION: Maintain a Redis token blacklist. On logout,
 *    add the token's JTI (JWT ID) to Redis with TTL = token expiry.
 *    Check blacklist on every request. We note this as accepted technical debt.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * Constructor injection of config values from application.yml.
     *
     * DECISION: Constructor injection over @Autowired field injection.
     * WHY: Constructor injection makes dependencies explicit and
     * allows the class to be instantiated in unit tests without
     * a Spring context — just pass the values directly.
     * Field injection hides dependencies and makes testing harder.
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        // DECISION: Derive the signing key from the configured secret string.
        // Keys.hmacShaKeyFor requires at least 256 bits (32 bytes) for HS256.
        // Our configured secret is 512 bits — sufficient for HS512 too.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a JWT token for an authenticated user.
     *
     * @param userId the authenticated user's UUID
     * @param email  the authenticated user's email (included as a claim for convenience)
     * @return signed JWT string
     */
    public String generateToken(UUID userId, String email) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                // subject = UUID string — stable, immutable identifier
                .subject(userId.toString())
                // email claim — convenient for logging/debugging without a DB lookup
                // SECURITY NOTE: Do not put sensitive data in JWT claims.
                // Claims are base64 encoded, NOT encrypted. Anyone can decode them.
                // Never put passwords, full account numbers, or PII beyond email.
                .claim("email", email)
                // issued-at timestamp
                .issuedAt(new Date(now))
                // expiry
                .expiration(new Date(now + expirationMs))
                // sign with our HMAC key
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the user ID (subject) from a token.
     * Returns null if the token is invalid or expired.
     *
     * @param token the JWT string (without "Bearer " prefix)
     * @return UUID of the token's subject, or null if invalid
     */
    public UUID extractUserId(String token) {
        try {
            String subject = extractAllClaims(token).getSubject();
            return UUID.fromString(subject);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validates a token against a UserDetails object.
     *
     * Checks:
     * 1. Token subject matches the UserDetails username (which we set to UUID string)
     * 2. Token is not expired
     *
     * @param token       the JWT string
     * @param userDetails loaded from the database
     * @return true if the token is valid for this user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            String subject = claims.getSubject();
            boolean notExpired = claims.getExpiration().after(new Date());
            // UserDetails.getUsername() returns the UUID string in our implementation
            return subject.equals(userDetails.getUsername()) && notExpired;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired for user");
            return false;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parses and returns all claims from a token.
     * Throws JwtException if the token is malformed, expired, or signature is invalid.
     *
     * DECISION: Private method — callers use the public extraction methods.
     * WHY: Encapsulates the JJWT API. If we ever swap JWT libraries,
     * only this method changes, not every caller.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

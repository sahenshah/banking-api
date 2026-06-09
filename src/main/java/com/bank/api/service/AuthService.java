package com.bank.api.service;

import com.bank.api.domain.User;
import com.bank.api.dto.request.LoginRequest;
import com.bank.api.dto.request.RegisterRequest;
import com.bank.api.dto.response.AuthResponse;
import com.bank.api.exception.ConflictException;
import com.bank.api.repository.UserRepository;
import com.bank.api.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and authentication.
 *
 * DESIGN DECISIONS:
 *
 * 1. @Transactional on register() — registration involves a DB write.
 *    If anything fails after the save (e.g. token generation), the
 *    transaction rolls back and the user is not partially created.
 *    Atomicity is essential here.
 *
 * 2. AuthenticationManager for login — we delegate credential verification
 *    to Spring Security's AuthenticationManager rather than manually
 *    fetching the user and comparing passwords.
 *    WHY: AuthenticationManager handles all the edge cases: disabled accounts,
 *    locked accounts, bad credentials. It's battle-tested and throws the
 *    right Spring Security exceptions that our GlobalExceptionHandler catches.
 *
 * 3. Password hashed in the service, not the controller.
 *    WHY: The service is the business logic layer. Hashing is a business
 *    rule ("passwords must be stored securely"). Controllers should not
 *    contain business logic.
 *
 * 4. existsByEmail check before save.
 *    WHY: The DB has a unique constraint on email as the ultimate guard,
 *    but checking first gives us a clean ConflictException with a helpful
 *    message rather than a DataIntegrityViolationException from the DB.
 *    RACE CONDITION NOTE: There is a TOCTOU (time-of-check-time-of-use)
 *    race condition here — two simultaneous registrations with the same
 *    email could both pass the check and then one fails at the DB constraint.
 *    The DB constraint is the real guard; our check is for UX only.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final long jwtExpirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Registers a new user and returns a JWT.
     *
     * @param request validated registration data
     * @return JWT token for immediate use
     * @throws ConflictException if email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                // Hash immediately — plaintext never touches the DB
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);
        log.debug("Registered new user: {}", user.getId());

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, jwtExpirationMs);
    }

    /**
     * Authenticates a user and returns a JWT.
     *
     * Delegates to Spring Security's AuthenticationManager which:
     * 1. Calls UserDetailsService.loadUserByUsername(email)
     * 2. Verifies the password matches the stored BCrypt hash
     * 3. Checks account is enabled
     * 4. Throws BadCredentialsException or DisabledException on failure
     *
     * @param request login credentials
     * @return JWT token
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // This line triggers Spring Security's full authentication flow.
        // If credentials are wrong it throws BadCredentialsException —
        // caught by GlobalExceptionHandler → 401 Unauthorized.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // After successful authentication, load user for token generation
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        log.debug("User logged in: {}", user.getId());

        return AuthResponse.of(token, jwtExpirationMs);
    }
}

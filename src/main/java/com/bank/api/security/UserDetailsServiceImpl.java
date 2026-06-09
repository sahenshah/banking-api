package com.bank.api.security;

import com.bank.api.domain.User;
import com.bank.api.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bridges Spring Security and our User domain.
 *
 * <p>DESIGN DECISIONS:
 *
 * <p>1. Implements UserDetailsService — Spring Security's standard integration point.
 *    Spring Security auto-detects this bean and uses it for authentication.
 *
 * <p>2. Dual lookup strategy — email OR UUID.
 *    The same loadUserByUsername() method is called in two different contexts:
 *    - LOGIN: AuthController calls it with an email address
 *    - JWT FILTER: JwtAuthFilter calls it with a UUID string (extracted from token)
 *    We handle both by attempting UUID parsing first, falling back to email lookup.
 *
 * <p>3. UserDetails.getUsername() = UUID string.
 *    After authentication, SecurityContext stores this principal.
 *    All downstream authorization checks extract the UUID from here.
 *    Using UUID (not email) means the identity is stable even if email changes.
 *
 * <p>4. No roles for this exercise — single ROLE_USER.
 *    PRODUCTION DIFFERENCE: CUSTOMER, TELLER, BRANCH_MANAGER, ADMIN roles
 *    enforced via @PreAuthorize and method security annotations.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by email (login) or UUID string (JWT filter).
     *
     * @param identifier either an email address or a UUID string
     * @return UserDetails with UUID as username, hashed password, and authorities
     * @throws UsernameNotFoundException if no matching user is found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = tryLoadByUuid(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + identifier
                ));

        return org.springframework.security.core.userdetails.User.builder()
                // username = UUID string — stable identifier for SecurityContext
                .username(user.getId().toString())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .disabled(!user.isEnabled())
                .build();
    }

    /**
     * Attempts to parse the identifier as a UUID and load from DB.
     * Returns empty Optional if identifier is not a valid UUID format.
     */
    private Optional<User> tryLoadByUuid(String identifier) {
        try {
            UUID uuid = UUID.fromString(identifier);
            return userRepository.findByIdAndEnabledTrue(uuid);
        } catch (IllegalArgumentException e) {
            // Not a UUID — fall through to email lookup
            return Optional.empty();
        }
    }
}

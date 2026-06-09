package com.bank.api.repository;

import com.bank.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for User entities.
 *
 * <p>DESIGN DECISIONS:
 *
 * <p>1. Extends JpaRepository<User, UUID>.
 *    JpaRepository gives us CRUD operations for free:
 *    save(), findById(), findAll(), delete(), count() etc.
 *    No implementation needed — Spring Data generates it at runtime.
 *
 * <p>2. findByEmail() — derived query method.
 *    Spring Data parses the method name and generates the JPQL:
 *    "SELECT u FROM User u WHERE u.email = :email"
 *    No @Query annotation needed for simple lookups like this.
 *    INTERVIEW TALKING POINT: "I use derived queries for simple lookups
 *    and @Query for anything complex. Derived queries are readable but
 *    can become unwieldy for multi-condition queries."
 *
 * <p>3. Returns Optional<User> — never null.
 *    Returning null from a repository method is an anti-pattern.
 *    Optional forces the caller to explicitly handle the not-found case,
 *    preventing NullPointerExceptions.
 *
 * <p>4. existsByEmail() — used during registration to check for duplicates.
 *    More efficient than findByEmail() for existence checks — the DB
 *    only needs to find one row, not load the full entity.
 *
 * <p>PRODUCTION NOTE: In a large system you'd also consider:
 *    - @QueryHints for read-only queries (performance)
 *    - Pagination for findAll() — never return unbounded lists
 *    - Projections for queries that only need a subset of fields
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     * Used by UserDetailsServiceImpl for authentication.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email already exists.
     * Used during registration to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);

    /**
     * Finds an enabled user by their UUID.
     * Used by JwtAuthFilter after extracting the UUID from the token.
     * Only returns enabled users — disabled accounts are rejected at the filter.
     */
    Optional<User> findByIdAndEnabledTrue(UUID id);
}

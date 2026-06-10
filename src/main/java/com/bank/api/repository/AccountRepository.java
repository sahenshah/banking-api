package com.bank.api.repository;

import com.bank.api.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for Account entities.
 *
 * DESIGN DECISIONS:
 *
 * 1. findByIdAndOwnerId — never find an account by ID alone.
 *    Always scope to the owner. This prevents IDOR (Insecure Direct
 *    Object Reference) at the repository level — even if the service
 *    accidentally passes the wrong ID, the query won't return another
 *    user's account because the owner filter is always applied.
 *    INTERVIEW TALKING POINT: "Authorization is enforced at the query
 *    level, not just in business logic. Defence in depth."
 *
 * 2. findAllByOwnerId — list only the authenticated user's accounts.
 *    Never expose findAll() — that would return every account in the DB.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Finds an account by ID scoped to a specific owner.
     * Returns empty if the account doesn't exist OR belongs to another user.
     */
    Optional<Account> findByIdAndOwner_Id(UUID id, UUID ownerId);

    /**
     * Returns all accounts belonging to a specific user.
     */
    List<Account> findAllByOwner_Id(UUID ownerId);

    /**
     * Checks if an account number is already taken.
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Finds an account by ID regardless of owner.
     * Used to distinguish 404 (not found) from 403 (found but forbidden).
     */
    Optional<Account> findById(UUID id);
}

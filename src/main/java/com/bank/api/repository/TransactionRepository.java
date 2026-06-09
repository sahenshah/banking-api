package com.bank.api.repository;

import com.bank.api.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for Transaction entities.
 *
 * DESIGN DECISIONS:
 *
 * 1. All queries scoped to accountId — transactions are always
 *    accessed in the context of an account. The service layer
 *    verifies account ownership before calling these methods,
 *    so scoping to accountId is sufficient.
 *
 * 2. findAllByAccount_IdOrderByCreatedAtDesc — most recent first.
 *    Bank statements conventionally show newest transactions at the top.
 *    Ordering in the query is more efficient than sorting in Java.
 *
 * PRODUCTION NOTE: In a real bank, transaction lists would be paginated.
 * An account with 10 years of transactions could have millions of records.
 * We would use Pageable here: findAllByAccount_Id(UUID accountId, Pageable pageable)
 * We omit pagination here for simplicity but would flag it in an interview.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Returns all transactions for an account, newest first.
     */
    List<Transaction> findAllByAccount_IdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Finds a specific transaction scoped to an account.
     * Returns empty if transaction doesn't exist or belongs to a different account.
     */
    Optional<Transaction> findByIdAndAccount_Id(UUID id, UUID accountId);
}

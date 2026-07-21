package com.bank.api.service;

import com.bank.api.dto.request.CreateTransferRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Handles account-to-account transfers.
 *
 * CONCURRENCY DECISION: Balance updates rely on JPA optimistic locking
 * (Account.version). Two concurrent transfers touching the same account
 * would otherwise race on a read-modify-write of the balance field and
 * silently lose one update. Instead of trusting a single attempt, each
 * transfer is retried up to MAX_ATTEMPTS times on
 * ObjectOptimisticLockingFailureException. Each attempt runs in its own
 * fresh transaction (see TransferExecutor) so the retry re-reads the
 * account and sees the latest committed balance, rather than reusing a
 * stale, detached entity. After exhausting retries we fail loudly (409)
 * rather than risk a lost update — silent data loss is worse than a
 * client-visible retry.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final TransferExecutor transferExecutor;

    public TransferService(TransferExecutor transferExecutor) {
        this.transferExecutor = transferExecutor;
    }

    public TransactionResponse transfer(UUID fromAccountId, UUID userId, CreateTransferRequest request) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return transferExecutor.execute(
                        fromAccountId, request.toAccountId(), userId, request, correlationId
                );
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error(
                            "Transfer failed permanently after {} attempts due to optimistic lock contention. "
                                    + "from_account_id={} to_account_id={} correlation_id={}",
                            MAX_ATTEMPTS, fromAccountId, request.toAccountId(), correlationId, ex
                    );
                    throw new ConflictException(
                            "Transfer could not be completed due to concurrent updates to the account. Please retry."
                    );
                }
                log.warn(
                        "Optimistic lock conflict on transfer attempt {}/{}, retrying. "
                                + "from_account_id={} to_account_id={} correlation_id={}",
                        attempt, MAX_ATTEMPTS, fromAccountId, request.toAccountId(), correlationId
                );
            }
        }

        // Unreachable: loop always returns or throws.
        throw new IllegalStateException("Transfer retry loop exited without result");
    }
}

package com.bank.api.dto.response;

import com.bank.api.domain.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for account endpoints.
 *
 * DESIGN DECISIONS:
 *
 * 1. version field is NOT included — internal concurrency mechanism,
 *    never exposed to clients.
 *
 * 2. ownerId included — useful for the client to correlate with user data.
 *    We expose the UUID, never the full User object (avoids deep nesting
 *    and accidental data leakage).
 *
 * 3. balance returned as BigDecimal — serialised as a JSON number with
 *    full precision. Never round or truncate monetary values in responses.
 */
public record AccountResponse(
        UUID id,
        String accountNumber,
        Account.AccountType accountType,
        BigDecimal balance,
        String currency,
        UUID ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse fromAccount(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getOwner().getId(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}

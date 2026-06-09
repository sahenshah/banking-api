package com.bank.api.dto.response;

import com.bank.api.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for transaction endpoints.
 *
 * DESIGN DECISIONS:
 *
 * 1. balanceAfter included — allows clients to reconstruct the balance
 *    at any point in history from the transaction list alone.
 *    Also useful for displaying running balance on bank statements.
 *
 * 2. accountId included — useful context, especially when transactions
 *    from multiple accounts might be viewed together in future.
 *
 * 3. No updatedAt — transactions are immutable, so there is no update time.
 *    Absence of the field signals immutability to the API consumer.
 */
public record TransactionResponse(
        UUID id,
        Transaction.TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        UUID accountId,
        LocalDateTime createdAt
) {
    public static TransactionResponse fromTransaction(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getAccount().getId(),
                transaction.getCreatedAt()
        );
    }
}

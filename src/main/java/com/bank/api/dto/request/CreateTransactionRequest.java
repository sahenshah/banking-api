package com.bank.api.dto.request;

import com.bank.api.domain.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request body for POST /v1/accounts/{accountId}/transactions
 *
 * DECISION: Single endpoint for both deposit and withdrawal.
 * The transaction type is specified in the request body.
 * This matches the spec and is cleaner than two separate endpoints.
 */
public record CreateTransactionRequest(

        @NotNull(message = "Transaction type is required")
        Transaction.TransactionType type,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        String description

) {}
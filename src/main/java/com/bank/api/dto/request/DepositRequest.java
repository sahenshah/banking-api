package com.bank.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/accounts/{id}/transactions/deposit
 *
 * DECISION: @DecimalMin("0.01") — minimum deposit of 1 penny.
 * Prevents zero and negative deposits at the validation layer
 * before business logic even runs. Belt-and-suspenders approach —
 * the Account.deposit() method also validates this.
 */
public record DepositRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Deposit amount must be at least 0.01")
        BigDecimal amount,

        String description

) {}

package com.bank.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/accounts/{id}/transactions/withdraw
 */
public record WithdrawRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Withdrawal amount must be at least 0.01")
        BigDecimal amount,

        String description

) {}

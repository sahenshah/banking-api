package com.bank.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for POST /v1/accounts/{accountId}/transfers
 */
public record CreateTransferRequest(

        @NotNull(message = "Destination account is required")
        UUID toAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        String description

) {}

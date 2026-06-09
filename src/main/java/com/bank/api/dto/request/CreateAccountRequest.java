package com.bank.api.dto.request;

import com.bank.api.domain.Account;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/v1/accounts
 *
 * DECISION: accountType is the only required field.
 * WHY: The account number is generated server-side (never client-provided),
 * balance starts at zero, currency defaults to GBP, and owner comes
 * from the JWT. The client only decides what type of account they want.
 */
public record CreateAccountRequest(

        @NotNull(message = "Account type is required")
        Account.AccountType accountType

) {}

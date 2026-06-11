package com.bank.api.dto.request;

import com.bank.api.domain.Account;

/**
 * Request body for PATCH /v1/accounts/{accountId}
 *
 * DECISION: Only accountType is updatable.
 * Balance is modified via transactions only — never directly.
 * Currency changes are a complex domain operation — out of scope.
 */
public record UpdateAccountRequest(
        Account.AccountType accountType
) {}
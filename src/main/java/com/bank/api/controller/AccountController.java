package com.bank.api.controller;

import com.bank.api.dto.request.CreateAccountRequest;
import com.bank.api.dto.response.AccountResponse;
import com.bank.api.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Handles account CRUD operations.
 * All endpoints require authentication.
 *
 * DESIGN DECISIONS:
 *
 * 1. userId always extracted from JWT, never from request body or path.
 *    A user cannot claim to be someone else by passing a different userId.
 *    The JWT is the single source of identity truth.
 *
 * 2. No /accounts/{userId}/{accountId} pattern.
 *    The userId is implicit from the JWT. Putting it in the URL would
 *    be redundant and create an IDOR surface — client could pass any userId.
 */
@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * POST /api/v1/accounts
     * Creates a new account for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        AccountResponse response = accountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/accounts
     * Lists all accounts belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<AccountResponse> accounts = accountService.listAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * GET /api/v1/accounts/{id}
     * Returns a specific account — only if owned by the authenticated user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        AccountResponse response = accountService.getAccount(id, userId);
        return ResponseEntity.ok(response);
    }
}

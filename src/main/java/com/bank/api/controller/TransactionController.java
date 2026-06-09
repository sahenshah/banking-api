package com.bank.api.controller;

import com.bank.api.dto.request.DepositRequest;
import com.bank.api.dto.request.WithdrawRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Handles deposit, withdrawal, and transaction history.
 * All endpoints require authentication.
 *
 * DESIGN DECISION: Transactions nested under /accounts/{accountId}/transactions
 * WHY: A transaction only makes sense in the context of an account.
 * The URL hierarchy makes this relationship explicit and RESTful.
 * It also naturally scopes all transaction operations to a specific account,
 * preventing cross-account transaction access in the URL design itself.
 */
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * POST /api/v1/accounts/{accountId}/transactions/deposit
     */
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse response = transactionService.deposit(accountId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/accounts/{accountId}/transactions/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse response = transactionService.withdraw(accountId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/accounts/{accountId}/transactions
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<TransactionResponse> transactions = transactionService.listTransactions(accountId, userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/v1/accounts/{accountId}/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable UUID accountId,
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse response = transactionService.getTransaction(accountId, transactionId, userId);
        return ResponseEntity.ok(response);
    }
}

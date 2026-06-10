package com.bank.api.controller;

import com.bank.api.dto.request.CreateTransactionRequest;
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

@RestController
@RequestMapping("/v1/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * POST /v1/accounts/{accountId}/transactions
     * Creates a deposit or withdrawal transaction.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreateTransactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse response = transactionService.createTransaction(
                accountId, userId, request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /v1/accounts/{accountId}/transactions
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<TransactionResponse> transactions = transactionService
                .listTransactions(accountId, userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /v1/accounts/{accountId}/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable UUID accountId,
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse response = transactionService.getTransaction(
                accountId, transactionId, userId
        );
        return ResponseEntity.ok(response);
    }
}
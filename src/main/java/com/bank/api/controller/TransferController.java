package com.bank.api.controller;

import com.bank.api.dto.request.CreateTransferRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts/{accountId}/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * POST /v1/accounts/{accountId}/transfers
     * Transfers funds from {accountId} to request.toAccountId().
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreateTransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TransactionResponse response = transferService.transfer(accountId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

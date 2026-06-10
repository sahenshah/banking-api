package com.bank.api.service;

import com.bank.api.domain.Account;
import com.bank.api.domain.Transaction;
import com.bank.api.dto.request.CreateTransactionRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    /**
     * Creates a deposit or withdrawal transaction.
     * Atomically updates balance and creates audit record.
     *
     * DECISION: Single method handles both types.
     * WHY: Matches the spec — one endpoint, type in body.
     * The Account entity's deposit()/withdraw() methods
     * enforce the business rules for each type.
     */
    @Transactional
    public TransactionResponse createTransaction(
            UUID accountId,
            UUID userId,
            CreateTransactionRequest request
    ) {
        Account account = accountService.loadAccountForOwner(accountId, userId);

        switch (request.type()) {
            case DEPOSIT -> account.deposit(request.amount());
            case WITHDRAWAL -> account.withdraw(request.amount());
        }

        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionType(request.type())
                .amount(request.amount())
                .balanceAfter(account.getBalance())
                .description(request.description())
                .account(account)
                .build();

        transactionRepository.save(transaction);

        return TransactionResponse.fromTransaction(transaction);
    }

    /**
     * Lists all transactions for an account, newest first.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(UUID accountId, UUID userId) {
        accountService.loadAccountForOwner(accountId, userId);
        return transactionRepository
                .findAllByAccount_IdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(TransactionResponse::fromTransaction)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a single transaction.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(
            UUID accountId,
            UUID transactionId,
            UUID userId
    ) {
        accountService.loadAccountForOwner(accountId, userId);
        Transaction transaction = transactionRepository
                .findByIdAndAccount_Id(transactionId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return TransactionResponse.fromTransaction(transaction);
    }
}
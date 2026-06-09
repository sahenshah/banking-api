package com.bank.api.service;

import com.bank.api.domain.Account;
import com.bank.api.domain.Transaction;
import com.bank.api.dto.request.DepositRequest;
import com.bank.api.dto.request.WithdrawRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles deposits, withdrawals, and transaction history.
 *
 * CRITICAL DESIGN — @Transactional on deposit and withdraw:
 *
 * Both operations involve TWO writes that must succeed or fail together:
 * 1. Update Account.balance
 * 2. Create a Transaction record
 *
 * If write 1 succeeds but write 2 fails (e.g. DB error), we'd have a
 * balance change with no audit record — a serious inconsistency.
 * @Transactional guarantees atomicity: both writes commit together
 * or neither does.
 *
 * OPTIMISTIC LOCKING:
 * Account has @Version. If two concurrent withdrawals both load the
 * same account, the second one to commit will find the version has
 * changed and throw OptimisticLockException. Spring's @Transactional
 * causes this to surface as a rollback. The client receives a 500
 * which we could improve to 409 Conflict with more error handling.
 * For this exercise we note it as a known improvement.
 *
 * INTERVIEW TALKING POINT: "The transaction log and account balance
 * are updated atomically. This is the most important correctness
 * guarantee in the system — you can never have a balance update
 * without a corresponding audit record."
 */
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
     * Deposits money into an account.
     * Atomically updates balance and creates audit record.
     *
     * @param accountId target account
     * @param userId    must be the account owner
     * @param request   deposit amount and optional description
     */
    @Transactional
    public TransactionResponse deposit(UUID accountId, UUID userId, DepositRequest request) {
        // Load account — verifies ownership, throws 404 if not found/not owned
        Account account = accountService.loadAccountForOwner(accountId, userId);

        // Business logic on the entity — validates amount, updates balance
        account.deposit(request.amount());

        // Save updated balance
        accountRepository.save(account);

        // Create immutable audit record with balance snapshot
        Transaction transaction = Transaction.builder()
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(request.amount())
                .balanceAfter(account.getBalance())
                .description(request.description())
                .account(account)
                .build();

        transactionRepository.save(transaction);

        return TransactionResponse.fromTransaction(transaction);
    }

    /**
     * Withdraws money from an account.
     * Atomically updates balance and creates audit record.
     * Throws InsufficientFundsException if balance would go negative.
     *
     * @param accountId source account
     * @param userId    must be the account owner
     * @param request   withdrawal amount and optional description
     */
    @Transactional
    public TransactionResponse withdraw(UUID accountId, UUID userId, WithdrawRequest request) {
        Account account = accountService.loadAccountForOwner(accountId, userId);

        // Business logic — validates amount and checks sufficient funds
        // Throws InsufficientFundsException if balance < amount
        // GlobalExceptionHandler maps this to 422 Unprocessable Entity
        account.withdraw(request.amount());

        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
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
     * Verifies account ownership before returning data.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(UUID accountId, UUID userId) {
        // Verify the account exists and belongs to this user
        accountService.loadAccountForOwner(accountId, userId);

        return transactionRepository
                .findAllByAccount_IdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(TransactionResponse::fromTransaction)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a single transaction.
     * Verifies both account ownership and that the transaction
     * belongs to that account.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID accountId, UUID transactionId, UUID userId) {
        // Verify account ownership first
        accountService.loadAccountForOwner(accountId, userId);

        // Then fetch the transaction scoped to this account
        Transaction transaction = transactionRepository
                .findByIdAndAccount_Id(transactionId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        return TransactionResponse.fromTransaction(transaction);
    }
}

package com.bank.api.service;

import com.bank.api.domain.Account;
import com.bank.api.domain.Transaction;
import com.bank.api.dto.request.CreateTransferRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.exception.InsufficientFundsException;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Executes a single transfer attempt inside its own transaction.
 *
 * DECISION: Split out from TransferService (rather than a self-invoked
 * @Transactional method on the same bean) because Spring's transaction
 * proxying only intercepts calls that go through the bean proxy. A
 * same-class call (this.doTransfer(...)) bypasses the proxy entirely and
 * @Transactional would silently do nothing — a classic Spring pitfall.
 * Calling through a separate bean guarantees REQUIRES_NEW actually opens
 * a fresh transaction per retry attempt.
 */
@Service
public class TransferExecutor {

    private static final Logger log = LoggerFactory.getLogger(TransferExecutor.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransferExecutor(AccountRepository accountRepository,
                             TransactionRepository transactionRepository,
                             AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionResponse execute(
            UUID fromAccountId,
            UUID toAccountId,
            UUID userId,
            CreateTransferRequest request,
            String correlationId
    ) {
        Account fromAccount;
        try {
            fromAccount = accountService.loadAccountForOwner(fromAccountId, userId);
        } catch (ResourceNotFoundException ex) {
            log.warn(
                    "Transfer failed - source account not found. "
                            + "from_account_id={} to_account_id={} correlation_id={}",
                    fromAccountId, toAccountId, correlationId
            );
            throw ex;
        }

        if (fromAccount.getId().equals(toAccountId)) {
            log.warn(
                    "Rejected transfer to same account. account_id={} correlation_id={}",
                    fromAccountId, correlationId
            );
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> {
                    log.warn(
                            "Transfer failed - destination account not found. "
                                    + "from_account_id={} to_account_id={} correlation_id={}",
                            fromAccountId, toAccountId, correlationId
                    );
                    return new ResourceNotFoundException("Destination account not found");
                });

        try {
            fromAccount.withdraw(request.amount());
        } catch (InsufficientFundsException ex) {
            log.warn(
                    "Transfer failed - insufficient funds. account_id={} amount={} correlation_id={}",
                    fromAccountId, request.amount(), correlationId
            );
            throw ex;
        }

        toAccount.deposit(request.amount());

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction outgoing = Transaction.builder()
                .transactionType(Transaction.TransactionType.TRANSFER_OUT)
                .amount(request.amount())
                .balanceAfter(fromAccount.getBalance())
                .description(request.description())
                .account(fromAccount)
                .build();
        transactionRepository.save(outgoing);

        Transaction incoming = Transaction.builder()
                .transactionType(Transaction.TransactionType.TRANSFER_IN)
                .amount(request.amount())
                .balanceAfter(toAccount.getBalance())
                .description(request.description())
                .account(toAccount)
                .build();
        transactionRepository.save(incoming);

        log.info(
                "Transfer succeeded. from_account_id={} to_account_id={} amount={} correlation_id={}",
                fromAccountId, toAccountId, request.amount(), correlationId
        );

        return TransactionResponse.fromTransaction(outgoing);
    }
}

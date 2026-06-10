package com.bank.api.service;

import com.bank.api.domain.Account;
import com.bank.api.domain.Transaction;
import com.bank.api.dto.request.CreateTransactionRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.exception.InsufficientFundsException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID accountId;
    private Account account;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        account = Account.builder()
                .accountNumber("GB123456789")
                .accountType(Account.AccountType.CURRENT)
                .build();
        account.deposit(new BigDecimal("500.00"));

        when(accountService.loadAccountForOwner(accountId, userId)).thenReturn(account);
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArgument(0));
        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Deposit should return transaction with correct amount and type")
    void deposit_shouldReturnCorrectTransaction() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                Transaction.TransactionType.DEPOSIT,
                new BigDecimal("200.00"),
                "Test deposit"
        );
        TransactionResponse response = transactionService.createTransaction(
                accountId, userId, request
        );
        assertThat(response.transactionType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(response.amount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Deposit should record correct balance after transaction")
    void deposit_shouldRecordCorrectBalanceAfter() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                Transaction.TransactionType.DEPOSIT,
                new BigDecimal("200.00"),
                "Test deposit"
        );
        TransactionResponse response = transactionService.createTransaction(
                accountId, userId, request
        );
        assertThat(response.balanceAfter()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("Deposit should save both the updated account and the transaction")
    void deposit_shouldSaveAccountAndTransaction() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                Transaction.TransactionType.DEPOSIT,
                new BigDecimal("100.00"),
                "Test"
        );
        transactionService.createTransaction(accountId, userId, request);
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Withdrawal should return transaction with correct amount and type")
    void withdraw_shouldReturnCorrectTransaction() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                Transaction.TransactionType.WITHDRAWAL,
                new BigDecimal("100.00"),
                "Test withdrawal"
        );
        TransactionResponse response = transactionService.createTransaction(
                accountId, userId, request
        );
        assertThat(response.transactionType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Withdrawal should record correct balance after transaction")
    void withdraw_shouldRecordCorrectBalanceAfter() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                Transaction.TransactionType.WITHDRAWAL,
                new BigDecimal("150.00"),
                "Test withdrawal"
        );
        TransactionResponse response = transactionService.createTransaction(
                accountId, userId, request
        );
        assertThat(response.balanceAfter()).isEqualByComparingTo("350.00");
    }

    @Test
    @DisplayName("Withdrawal with insufficient funds should throw and not save anything")
    void withdraw_insufficientFunds_shouldThrowAndNotSave() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                Transaction.TransactionType.WITHDRAWAL,
                new BigDecimal("999.00"),
                "Should fail"
        );
        assertThatThrownBy(() -> transactionService.createTransaction(
                accountId, userId, request
        )).isInstanceOf(InsufficientFundsException.class);
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdrawal should save both the updated account and the transaction")
    void withdraw_shouldSaveAccountAndTransaction() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                Transaction.TransactionType.WITHDRAWAL,
                new BigDecimal("100.00"),
                "Test"
        );
        transactionService.createTransaction(accountId, userId, request);
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }
}
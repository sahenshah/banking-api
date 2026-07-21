package com.bank.api.service;

import com.bank.api.domain.Account;
import com.bank.api.domain.User;
import com.bank.api.dto.request.CreateTransferRequest;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the concurrency bug fixed by adding optimistic
 * locking + retry to TransferService: two concurrent transfers debiting
 * the same source account, on a naive read-modify-write, would race and
 * lose one of the updates. This test proves the final balance reflects
 * BOTH transfers, not just that no exception escaped.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TransferService Concurrency Tests")
class TransferServiceConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private UUID sourceAccountId;
    private UUID destinationAccountAId;
    private UUID destinationAccountBId;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .email("concurrency-test-" + UUID.randomUUID() + "@example.com")
                .password("$2a$12$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1")
                .firstName("Test")
                .lastName("User")
                .build();
        userRepository.save(owner);

        Account source = Account.builder()
                .accountNumber("GB" + System.nanoTime())
                .accountType(Account.AccountType.CURRENT)
                .owner(owner)
                .build();
        source.deposit(new BigDecimal("1000.00"));
        owner.addAccount(source);
        accountRepository.save(source);
        sourceAccountId = source.getId();

        Account destA = Account.builder()
                .accountNumber("GB" + (System.nanoTime() + 1))
                .accountType(Account.AccountType.SAVINGS)
                .owner(owner)
                .build();
        owner.addAccount(destA);
        accountRepository.save(destA);
        destinationAccountAId = destA.getId();

        Account destB = Account.builder()
                .accountNumber("GB" + (System.nanoTime() + 2))
                .accountType(Account.AccountType.SAVINGS)
                .owner(owner)
                .build();
        owner.addAccount(destB);
        accountRepository.save(destB);
        destinationAccountBId = destB.getId();
    }

    @Test
    @DisplayName("Two concurrent transfers from the same account both apply exactly once")
    void concurrentTransfers_fromSameAccount_bothApplyCorrectly() throws Exception {
        BigDecimal transferAmount = new BigDecimal("100.00");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            var futureA = executor.submit(() -> {
                readyLatch.countDown();
                await(startLatch);
                transferService.transfer(
                        sourceAccountId,
                        owner.getId(),
                        new CreateTransferRequest(destinationAccountAId, transferAmount, "concurrent test A")
                );
                return null;
            });

            var futureB = executor.submit(() -> {
                readyLatch.countDown();
                await(startLatch);
                transferService.transfer(
                        sourceAccountId,
                        owner.getId(),
                        new CreateTransferRequest(destinationAccountBId, transferAmount, "concurrent test B")
                );
                return null;
            });

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            futureA.get(10, TimeUnit.SECONDS);
            futureB.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        Account source = accountRepository.findById(sourceAccountId).orElseThrow();
        Account destA = accountRepository.findById(destinationAccountAId).orElseThrow();
        Account destB = accountRepository.findById(destinationAccountBId).orElseThrow();

        assertThat(source.getBalance()).isEqualByComparingTo("800.00");
        assertThat(destA.getBalance()).isEqualByComparingTo("100.00");
        assertThat(destB.getBalance()).isEqualByComparingTo("100.00");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}

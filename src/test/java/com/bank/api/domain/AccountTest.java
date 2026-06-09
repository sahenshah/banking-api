package com.bank.api.domain;

import com.bank.api.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Account Domain Tests")
class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .accountNumber("GB123456789")
                .accountType(Account.AccountType.CURRENT)
                .build();
        account.deposit(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Deposit should increase balance by the deposited amount")
    void deposit_shouldIncreaseBalance() {
        account.deposit(new BigDecimal("250.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("1250.00");
    }

    @Test
    @DisplayName("Multiple deposits should accumulate correctly")
    void deposit_multipleDeposits_shouldAccumulate() {
        account.deposit(new BigDecimal("100.00"));
        account.deposit(new BigDecimal("200.00"));
        account.deposit(new BigDecimal("50.50"));
        assertThat(account.getBalance()).isEqualByComparingTo("1350.50");
    }

    @Test
    @DisplayName("Deposit of zero should throw IllegalArgumentException")
    void deposit_zeroAmount_shouldThrow() {
        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Deposit of negative amount should throw IllegalArgumentException")
    void deposit_negativeAmount_shouldThrow() {
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Deposit of null should throw IllegalArgumentException")
    void deposit_nullAmount_shouldThrow() {
        assertThatThrownBy(() -> account.deposit(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Withdrawal should decrease balance by the withdrawn amount")
    void withdraw_shouldDecreaseBalance() {
        account.withdraw(new BigDecimal("300.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("Withdrawal of entire balance should succeed and result in zero balance")
    void withdraw_entireBalance_shouldSucceed() {
        account.withdraw(new BigDecimal("1000.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Withdrawal exceeding balance should throw InsufficientFundsException")
    void withdraw_insufficientFunds_shouldThrow() {
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("1500.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("Withdrawal of zero should throw IllegalArgumentException")
    void withdraw_zeroAmount_shouldThrow() {
        assertThatThrownBy(() -> account.withdraw(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Withdrawal of negative amount should throw IllegalArgumentException")
    void withdraw_negativeAmount_shouldThrow() {
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("-100.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Balance should never go negative after failed withdrawal")
    void withdraw_failedWithdrawal_balanceShouldBeUnchanged() {
        BigDecimal balanceBefore = account.getBalance();
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("9999.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("isOwnedBy should return false when account has no owner")
    void isOwnedBy_noOwner_shouldReturnFalse() {
        java.util.UUID randomId = java.util.UUID.randomUUID();
        assertThat(account.isOwnedBy(randomId)).isFalse();
    }
}
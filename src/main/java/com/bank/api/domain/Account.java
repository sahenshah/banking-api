package com.bank.api.domain;

import com.bank.api.exception.InsufficientFundsException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a bank account owned by a User.
 *
 * DESIGN DECISIONS:
 * - BigDecimal balance: never float/double for money (IEEE 754 precision errors)
 * - @Version optimistic locking: prevents lost-update concurrency bug on withdrawals
 * - Rich domain model: deposit/withdraw business logic lives on the entity
 * - Explicit getters/setters: avoids Lombok annotation processing issues with JPA
 */
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_owner_id", columnList = "owner_id"),
                @Index(name = "idx_accounts_account_number", columnList = "account_number")
        }
)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GBP";

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(
            mappedBy = "account",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Transaction> transactions = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================

    protected Account() {}

    private Account(Builder builder) {
        this.accountNumber = builder.accountNumber;
        this.accountType = builder.accountType;
        this.balance = BigDecimal.ZERO;
        this.currency = builder.currency != null ? builder.currency : "GBP";
        this.owner = builder.owner;
    }

    // ==================== GETTERS ====================

    public UUID getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public AccountType getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public User getOwner() { return owner; }
    public List<Transaction> getTransactions() { return transactions; }

    // ==================== SETTERS ====================

    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setOwner(User owner) { this.owner = owner; }

    // ==================== BUSINESS METHODS ====================

    /**
     * Deposits an amount to this account.
     * Business logic on the entity — rich domain model pattern.
     * Invariants are enforced here, never bypassable via a different service method.
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Withdraws an amount from this account.
     * Throws InsufficientFundsException if balance would go negative.
     * Service layer catches this and maps it to HTTP 422.
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + this.balance + ", Requested: " + amount
            );
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Whether this account is owned by the given user.
     * Used in service-layer authorization checks.
     */
    public boolean isOwnedBy(UUID userId) {
        return this.owner != null && this.owner.getId().equals(userId);
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accountNumber;
        private AccountType accountType;
        private String currency;
        private User owner;

        public Builder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public Builder accountType(AccountType accountType) { this.accountType = accountType; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder owner(User owner) { this.owner = owner; return this; }
        public Account build() { return new Account(this); }
    }

    // ==================== ENUM ====================

    public enum AccountType {
        CURRENT,
        SAVINGS
    }
}

package com.bank.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an immutable financial transaction on an Account.
 *
 * IMMUTABILITY DESIGN:
 * - No setters — once created, a transaction cannot be modified
 * - No updatedAt — explicitly absent to signal immutability
 * - No @Version — optimistic locking only needed on mutable entities
 * - updatable=false on account join — relationship cannot change after creation
 *
 * BALANCEAFTER:
 * Stores the account balance immediately after this transaction as a snapshot.
 * Enables point-in-time balance reconstruction and regulatory reporting
 * without replaying the full transaction history.
 *
 * INTERVIEW TALKING POINT: "The transaction log is an append-only ledger —
 * same pattern as event sourcing and double-entry bookkeeping. Current balance
 * is cached on Account for read performance, but the transaction log is
 * the source of truth."
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_account_id", columnList = "account_id"),
                @Index(name = "idx_transactions_created_at", columnList = "created_at")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    // ==================== CONSTRUCTORS ====================

    protected Transaction() {}

    private Transaction(Builder builder) {
        this.transactionType = builder.transactionType;
        this.amount = builder.amount;
        this.balanceAfter = builder.balanceAfter;
        this.description = builder.description;
        this.account = builder.account;
    }

    // ==================== GETTERS ONLY — no setters, immutable ====================

    public UUID getId() { return id; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Account getAccount() { return account; }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TransactionType transactionType;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String description;
        private Account account;

        public Builder transactionType(TransactionType transactionType) { this.transactionType = transactionType; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder balanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder account(Account account) { this.account = account; return this; }
        public Transaction build() { return new Transaction(this); }
    }

    // ==================== ENUM ====================

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }
}

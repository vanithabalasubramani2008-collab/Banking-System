package com.securebank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an immutable financial transaction in SecureBank.
 * Stored persistently in MySQL `transactions` table.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_tx_account_created", columnList = "account_id, created_at DESC"),
    @Index(name = "idx_tx_reference", columnList = "transfer_reference")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "transfer_reference", length = 50)
    private String transferReference;

    @Column(name = "counterparty_account_number", length = 20)
    private String counterpartyAccountNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(Account account, TransactionType transactionType, BigDecimal amount,
                       BigDecimal balanceAfter, String description, String transferReference,
                       String counterpartyAccountNumber) {
        this.account = account;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.transferReference = transferReference;
        this.counterpartyAccountNumber = counterpartyAccountNumber;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public String getCounterpartyAccountNumber() {
        return counterpartyAccountNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

package com.securebank.entity;

import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.InvalidTransactionException;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Concrete entity representing a Current Account with Overdraft facility.
 * Demonstrates:
 * - OOP: Inheritance (extends Account)
 * - OOP: Constructor chaining via super(...)
 * - OOP: Method Overriding (withdraw, displayDetails)
 * - Business Rules: ₹5,000 Overdraft limit (balance can go down to -₹5,000)
 */
@Entity
@Table(name = "current_accounts")
@DiscriminatorValue("CURRENT")
public class CurrentAccount extends Account {

    /**
     * Final constant for maximum allowed overdraft limit (₹5,000).
     */
    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("5000.00");

    @Column(name = "overdraft_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal overdraftLimit = OVERDRAFT_LIMIT;

    protected CurrentAccount() {
        super();
    }

    /**
     * Constructor chaining using super.
     */
    public CurrentAccount(String accountNumber, User user, String accountHolderName, BigDecimal initialBalance) {
        super(accountNumber, user, accountHolderName, initialBalance, AccountType.CURRENT);
        this.overdraftLimit = OVERDRAFT_LIMIT;
    }

    /**
     * Overridden withdraw method.
     * Enforces that balance cannot drop below -OVERDRAFT_LIMIT (-₹5,000).
     */
    @Override
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be strictly greater than zero");
        }

        BigDecimal resultingBalance = getBalance().subtract(amount);
        BigDecimal maxAllowedNegativeBalance = this.overdraftLimit.negate();

        if (resultingBalance.compareTo(maxAllowedNegativeBalance) < 0) {
            BigDecimal maxAvailableFunds = getBalance().add(this.overdraftLimit);
            BigDecimal shortfall = amount.subtract(maxAvailableFunds);

            throw new InsufficientFundsException(
                String.format("Current account overdraft limit of ₹%s exceeded. Shortfall: ₹%s",
                        overdraftLimit.toPlainString(), shortfall.toPlainString()),
                getBalance(),
                amount,
                shortfall
            );
        }

        setBalance(resultingBalance);
    }

    @Override
    public String displayDetails() {
        return String.format(
            "Account Type: CURRENT | Account No: %s | Holder: %s | Balance: ₹%s | Overdraft Limit: ₹%s",
            getAccountNumber(), getAccountHolderName(), getBalance().toPlainString(), overdraftLimit.toPlainString()
        );
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
}

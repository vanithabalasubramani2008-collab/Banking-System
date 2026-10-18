package com.securebank.entity;

import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.InvalidTransactionException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete entity representing a Savings Account.
 * Demonstrates:
 * - OOP: Inheritance (extends Account)
 * - OOP: Constructor chaining via super(...)
 * - OOP: Method Overriding (withdraw, displayDetails)
 * - Business Rules: ₹1,000 Minimum Balance, 4% Annual Interest calculation
 */
@Entity
@Table(name = "savings_accounts")
@DiscriminatorValue("SAVINGS")
public class SavingsAccount extends Account {

    /**
     * Final constant for minimum required balance (₹1,000).
     */
    public static final BigDecimal MINIMUM_BALANCE = new BigDecimal("1000.00");

    /**
     * Final constant for yearly interest rate (4.00%).
     */
    public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.04");

    @Column(name = "minimum_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal minBalanceThreshold = MINIMUM_BALANCE;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate = ANNUAL_INTEREST_RATE;

    protected SavingsAccount() {
        super();
    }

    /**
     * Constructor chaining using super.
     */
    public SavingsAccount(String accountNumber, User user, String accountHolderName, BigDecimal initialBalance) {
        super(accountNumber, user, accountHolderName, initialBalance, AccountType.SAVINGS);
        if (initialBalance.compareTo(MINIMUM_BALANCE) < 0) {
            throw new InsufficientFundsException(
                "Initial balance for Savings Account cannot be less than minimum balance ₹" + MINIMUM_BALANCE,
                initialBalance,
                MINIMUM_BALANCE,
                MINIMUM_BALANCE.subtract(initialBalance)
            );
        }
        this.minBalanceThreshold = MINIMUM_BALANCE;
        this.interestRate = ANNUAL_INTEREST_RATE;
    }

    /**
     * Overridden withdraw method.
     * Enforces that balance cannot drop below MINIMUM_BALANCE (₹1,000).
     */
    @Override
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be strictly greater than zero");
        }

        BigDecimal resultingBalance = getBalance().subtract(amount);
        if (resultingBalance.compareTo(minBalanceThreshold) < 0) {
            BigDecimal allowedWithdrawal = getBalance().subtract(minBalanceThreshold);
            if (allowedWithdrawal.compareTo(BigDecimal.ZERO) < 0) {
                allowedWithdrawal = BigDecimal.ZERO;
            }
            BigDecimal shortfall = amount.subtract(allowedWithdrawal);

            throw new InsufficientFundsException(
                String.format("Savings account requires a minimum balance of ₹%s. Shortfall: ₹%s",
                        minBalanceThreshold.toPlainString(), shortfall.toPlainString()),
                getBalance(),
                amount,
                shortfall
            );
        }

        setBalance(resultingBalance);
    }

    /**
     * Dedicated method to calculate yearly 4% interest.
     * Formula: Balance * 0.04
     */
    public BigDecimal calculateYearlyInterest() {
        return getBalance().multiply(this.interestRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Credits calculated yearly interest to the account balance.
     */
    public BigDecimal applyYearlyInterest() {
        BigDecimal interest = calculateYearlyInterest();
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            deposit(interest);
        }
        return interest;
    }

    @Override
    public String displayDetails() {
        return String.format(
            "Account Type: SAVINGS | Account No: %s | Holder: %s | Balance: ₹%s | Min Balance: ₹%s | Interest Rate: 4%%",
            getAccountNumber(), getAccountHolderName(), getBalance().toPlainString(), minBalanceThreshold.toPlainString()
        );
    }

    public BigDecimal getMinBalanceThreshold() {
        return minBalanceThreshold;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }
}

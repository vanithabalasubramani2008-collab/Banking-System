package com.securebank.exception;

import java.math.BigDecimal;

/**
 * Custom runtime exception thrown when an account withdrawal or transfer
 * would violate minimum balance or overdraft limits.
 *
 * Demonstrates:
 * - Custom domain exception modeling
 * - Precise shortfall calculation for clear client reporting
 */
public class InsufficientFundsException extends RuntimeException {

    private final BigDecimal currentBalance;
    private final BigDecimal attemptedAmount;
    private final BigDecimal shortfall;

    public InsufficientFundsException(String message, BigDecimal currentBalance, BigDecimal attemptedAmount, BigDecimal shortfall) {
        super(message);
        this.currentBalance = currentBalance;
        this.attemptedAmount = attemptedAmount;
        this.shortfall = shortfall;
    }

    public InsufficientFundsException(String message) {
        this(message, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getAttemptedAmount() {
        return attemptedAmount;
    }

    public BigDecimal getShortfall() {
        return shortfall;
    }
}

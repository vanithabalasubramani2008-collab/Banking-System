package com.securebank.domain;

import com.securebank.entity.Account;
import com.securebank.entity.CurrentAccount;
import com.securebank.entity.SavingsAccount;
import com.securebank.entity.User;
import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.InvalidTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OOP Domain Rules & Inheritance Unit Tests")
class AccountDomainTest {

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("alice", "alice@example.com", "hashed_pwd", "hashed_pin", "Alice Smith", "9876543210", "ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Savings Account enforces ₹1,000 minimum balance on creation")
    void savingsAccountEnforcesMinimumBalanceOnCreation() {
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () -> {
            new SavingsAccount("SB10001", sampleUser, "Alice Smith", new BigDecimal("500.00"));
        });
        assertTrue(ex.getMessage().contains("minimum balance"));
        assertEquals(new BigDecimal("500.00"), ex.getShortfall());
    }

    @Test
    @DisplayName("Savings Account successful deposit and withdrawal preserving ₹1,000 minimum balance")
    void savingsAccountDepositAndWithdrawSuccess() {
        Account savings = new SavingsAccount("SB10002", sampleUser, "Alice Smith", new BigDecimal("5000.00"));
        savings.deposit(new BigDecimal("1500.00"));
        assertEquals(new BigDecimal("6500.00"), savings.getBalance());

        savings.withdraw(new BigDecimal("5500.00"));
        assertEquals(new BigDecimal("1000.00"), savings.getBalance());
    }

    @Test
    @DisplayName("Savings Account throws InsufficientFundsException with shortfall when violating minimum balance")
    void savingsAccountViolatingMinimumBalanceThrowsShortfall() {
        Account savings = new SavingsAccount("SB10003", sampleUser, "Alice Smith", new BigDecimal("2000.00"));

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () -> {
            savings.withdraw(new BigDecimal("1500.00"));
        });

        // Current balance = 2000, Min = 1000, Max withdrawable = 1000. Attempted = 1500, Shortfall = 500
        assertEquals(new BigDecimal("500.00"), ex.getShortfall());
        assertEquals(new BigDecimal("2000.00"), ex.getCurrentBalance());
        assertEquals(new BigDecimal("1500.00"), ex.getAttemptedAmount());
    }

    @Test
    @DisplayName("Savings Account calculates and applies 4% yearly interest correctly")
    void savingsAccountCalculatesYearlyInterest() {
        SavingsAccount savings = new SavingsAccount("SB10004", sampleUser, "Alice Smith", new BigDecimal("10000.00"));
        BigDecimal interest = savings.calculateYearlyInterest();
        assertEquals(new BigDecimal("400.00"), interest);

        savings.applyYearlyInterest();
        assertEquals(new BigDecimal("10400.00"), savings.getBalance());
    }

    @Test
    @DisplayName("Current Account allows overdraft up to ₹5,000")
    void currentAccountAllowsOverdraftLimit() {
        Account current = new CurrentAccount("CA20001", sampleUser, "Alice Smith", new BigDecimal("1000.00"));

        // Withdrawing 3000 should lead to balance of -2000 (within 5000 limit)
        current.withdraw(new BigDecimal("3000.00"));
        assertEquals(new BigDecimal("-2000.00"), current.getBalance());

        // Withdrawing another 3000 should lead to balance of -5000 (at exact overdraft limit)
        current.withdraw(new BigDecimal("3000.00"));
        assertEquals(new BigDecimal("-5000.00"), current.getBalance());
    }

    @Test
    @DisplayName("Current Account throws InsufficientFundsException when overdraft limit exceeded")
    void currentAccountOverdraftLimitExceededThrowsShortfall() {
        Account current = new CurrentAccount("CA20002", sampleUser, "Alice Smith", new BigDecimal("1000.00"));

        // Withdrawing 6500: Available = 1000 + 5000 = 6000. Shortfall = 500
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () -> {
            current.withdraw(new BigDecimal("6500.00"));
        });

        assertEquals(new BigDecimal("500.00"), ex.getShortfall());
        assertEquals(new BigDecimal("1000.00"), ex.getCurrentBalance());
        assertEquals(new BigDecimal("6500.00"), ex.getAttemptedAmount());
    }

    @Test
    @DisplayName("Polymorphism: displayDetails() invokes overridden subclass implementations")
    void polymorphismDisplayDetails() {
        Account savings = new SavingsAccount("SB10005", sampleUser, "Alice Smith", new BigDecimal("2500.00"));
        Account current = new CurrentAccount("CA20003", sampleUser, "Alice Smith", new BigDecimal("4000.00"));

        assertTrue(savings.displayDetails().contains("Account Type: SAVINGS"));
        assertTrue(savings.displayDetails().contains("Min Balance: ₹1000.00"));

        assertTrue(current.displayDetails().contains("Account Type: CURRENT"));
        assertTrue(current.displayDetails().contains("Overdraft Limit: ₹5000.00"));
    }

    @Test
    @DisplayName("Deposit and Withdrawal reject zero or negative amounts")
    void rejectsInvalidAmounts() {
        Account savings = new SavingsAccount("SB10006", sampleUser, "Alice Smith", new BigDecimal("2000.00"));

        assertThrows(InvalidTransactionException.class, () -> savings.deposit(BigDecimal.ZERO));
        assertThrows(InvalidTransactionException.class, () -> savings.deposit(new BigDecimal("-100.00")));
        assertThrows(InvalidTransactionException.class, () -> savings.withdraw(BigDecimal.ZERO));
        assertThrows(InvalidTransactionException.class, () -> savings.withdraw(new BigDecimal("-50.00")));
    }
}

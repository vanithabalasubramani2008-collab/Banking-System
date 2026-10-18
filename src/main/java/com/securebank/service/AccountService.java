package com.securebank.service;

import com.securebank.dto.AccountResponse;
import com.securebank.dto.AccountStatementResponse;
import com.securebank.dto.CreateAccountRequest;
import com.securebank.dto.TransactionResponse;
import com.securebank.entity.*;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.InvalidTransactionException;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          AuthService authService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, String username) {
        User user = authService.getCurrentAuthenticatedUser(username);

        // Security check: Verify PIN when creating an account
        authService.verifyPin(user, request.getPin());

        String accountNumber = generateUniqueAccountNumber();

        // Demonstrates Polymorphism and Object-Oriented inheritance instantiation
        Account account;
        if (request.getAccountType() == AccountType.SAVINGS) {
            account = new SavingsAccount(
                    accountNumber,
                    user,
                    user.getFullName(),
                    request.getInitialDeposit()
            );
        } else if (request.getAccountType() == AccountType.CURRENT) {
            account = new CurrentAccount(
                    accountNumber,
                    user,
                    user.getFullName(),
                    request.getInitialDeposit()
            );
        } else {
            throw new InvalidTransactionException("Unsupported account type: " + request.getAccountType());
        }

        Account savedAccount = accountRepository.save(account);

        // Record initial deposit as the first persistent transaction
        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            Transaction initialTx = new Transaction(
                    savedAccount,
                    TransactionType.DEPOSIT,
                    request.getInitialDeposit(),
                    savedAccount.getBalance(),
                    "Initial account opening deposit",
                    null,
                    null
            );
            transactionRepository.save(initialTx);
        }

        return mapToAccountResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsForUser(String username) {
        User user = authService.getCurrentAuthenticatedUser(username);
        return accountRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id, String username) {
        Account account = findAccountById(id);
        verifyOwnership(account, username);
        return mapToAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(Long accountId, String username, int limit) {
        Account account = findAccountById(accountId);
        verifyOwnership(account, username);

        int maxLimit = (limit > 0) ? limit : 5;
        return transactionRepository.findRecentTransactions(accountId, PageRequest.of(0, maxLimit))
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getAccountStatement(Long accountId, String username) {
        Account account = findAccountById(accountId);
        verifyOwnership(account, username);

        List<TransactionResponse> recent = transactionRepository.findRecentTransactions(accountId, PageRequest.of(0, 5))
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        List<Transaction> allTxs = transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        return new AccountStatementResponse(
                mapToAccountResponse(account),
                recent,
                allTxs.size()
        );
    }

    /**
     * Applies 4% annual interest to a Savings Account.
     * Demonstrates Polymorphism and Domain Rule enforcement.
     */
    @Transactional
    public TransactionResponse applyInterest(Long accountId, String username) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found"));

        verifyOwnership(account, username);

        if (!(account instanceof SavingsAccount savingsAccount)) {
            throw new InvalidTransactionException("Yearly interest calculation is only available for Savings Accounts");
        }

        BigDecimal interestCredited = savingsAccount.applyYearlyInterest();
        accountRepository.save(savingsAccount);

        Transaction tx = new Transaction(
                savingsAccount,
                TransactionType.INTEREST_CREDIT,
                interestCredited,
                savingsAccount.getBalance(),
                "Annual 4% Interest Credit",
                null,
                null
        );
        Transaction savedTx = transactionRepository.save(tx);

        return mapToTransactionResponse(savedTx);
    }

    public Account findAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
    }

    public void verifyOwnership(Account account, String username) {
        if (!account.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Access denied: You do not own account number " + account.getAccountNumber());
        }
    }

    private String generateUniqueAccountNumber() {
        String accNum;
        do {
            long randomNineDigits = 100000000L + (long) (secureRandom.nextDouble() * 900000000L);
            accNum = "SB" + randomNineDigits;
        } while (accountRepository.existsByAccountNumber(accNum));
        return accNum;
    }

    public AccountResponse mapToAccountResponse(Account account) {
        BigDecimal minBalance = null;
        BigDecimal overdraftLimit = null;
        BigDecimal interestRate = null;

        if (account instanceof SavingsAccount sa) {
            minBalance = sa.getMinBalanceThreshold();
            interestRate = sa.getInterestRate();
        } else if (account instanceof CurrentAccount ca) {
            overdraftLimit = ca.getOverdraftLimit();
        }

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getAccountType(),
                account.getBalance(),
                minBalance,
                overdraftLimit,
                interestRate,
                account.displayDetails(),
                account.getCreatedAt()
        );
    }

    public TransactionResponse mapToTransactionResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getAccount().getId(),
                tx.getAccount().getAccountNumber(),
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getDescription(),
                tx.getTransferReference(),
                tx.getCounterpartyAccountNumber(),
                tx.getCreatedAt()
        );
    }
}

package com.securebank.service;

import com.securebank.dto.DepositRequest;
import com.securebank.dto.TransactionResponse;
import com.securebank.dto.TransferRequest;
import com.securebank.dto.WithdrawalRequest;
import com.securebank.entity.Account;
import com.securebank.entity.Transaction;
import com.securebank.entity.TransactionType;
import com.securebank.entity.User;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.InvalidTransactionException;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;
    private final AccountService accountService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              AuthService authService,
                              AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
        this.accountService = accountService;
    }

    /**
     * Executes an authenticated deposit using pessimistic write locking.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse deposit(Long accountId, DepositRequest request, String username) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found"));

        accountService.verifyOwnership(account, username);

        // Core domain OOP logic: deposit delegates to polymorphic Account.deposit()
        account.deposit(request.getAmount());
        Account savedAccount = accountRepository.save(account);

        String description = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription()
                : "Cash / Online Deposit";

        Transaction tx = new Transaction(
                savedAccount,
                TransactionType.DEPOSIT,
                request.getAmount(),
                savedAccount.getBalance(),
                description,
                null,
                null
        );

        Transaction savedTx = transactionRepository.save(tx);
        return accountService.mapToTransactionResponse(savedTx);
    }

    /**
     * Executes an authenticated withdrawal with PIN verification and pessimistic locking.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdraw(Long accountId, WithdrawalRequest request, String username) {
        User user = authService.getCurrentAuthenticatedUser(username);
        authService.verifyPin(user, request.getPin());

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found"));

        accountService.verifyOwnership(account, username);

        // Core domain OOP logic: polymorphic call to withdraw() executes
        // SavingsAccount.withdraw() or CurrentAccount.withdraw()
        account.withdraw(request.getAmount());
        Account savedAccount = accountRepository.save(account);

        String description = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription()
                : "Cash / ATM Withdrawal";

        Transaction tx = new Transaction(
                savedAccount,
                TransactionType.WITHDRAWAL,
                request.getAmount(),
                savedAccount.getBalance(),
                description,
                null,
                null
        );

        Transaction savedTx = transactionRepository.save(tx);
        return accountService.mapToTransactionResponse(savedTx);
    }

    /**
     * Atomic fund transfer between two accounts with deadlock avoidance.
     * Guaranteed atomic rollback if debit, credit, or PIN verification fails.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransactionResponse transfer(TransferRequest request, String username) {
        if (request.getSourceAccountNumber().equalsIgnoreCase(request.getDestinationAccountNumber())) {
            throw new InvalidTransactionException("Source and destination accounts cannot be identical");
        }

        User user = authService.getCurrentAuthenticatedUser(username);
        authService.verifyPin(user, request.getPin());

        // Deadlock Prevention: Always acquire locks in canonical (alphanumeric) order
        String firstAccNo = request.getSourceAccountNumber().compareTo(request.getDestinationAccountNumber()) < 0
                ? request.getSourceAccountNumber()
                : request.getDestinationAccountNumber();
        String secondAccNo = firstAccNo.equals(request.getSourceAccountNumber())
                ? request.getDestinationAccountNumber()
                : request.getSourceAccountNumber();

        Account lockedFirst = accountRepository.findByAccountNumberWithLock(firstAccNo)
                .orElseThrow(() -> new AccountNotFoundException("Account " + firstAccNo + " not found"));

        Account lockedSecond = accountRepository.findByAccountNumberWithLock(secondAccNo)
                .orElseThrow(() -> new AccountNotFoundException("Account " + secondAccNo + " not found"));

        Account sourceAccount = lockedFirst.getAccountNumber().equalsIgnoreCase(request.getSourceAccountNumber())
                ? lockedFirst : lockedSecond;
        Account destinationAccount = lockedFirst.getAccountNumber().equalsIgnoreCase(request.getDestinationAccountNumber())
                ? lockedFirst : lockedSecond;

        // Verify that the authenticated caller owns the source account
        accountService.verifyOwnership(sourceAccount, username);

        // Debit source account (polymorphic withdraw validates rules)
        sourceAccount.withdraw(request.getAmount());

        // Credit destination account
        destinationAccount.deposit(request.getAmount());

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        // Generate shared transfer reference
        String transferRef = "TRF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        String note = (request.getDescription() != null && !request.getDescription().isBlank())
                ? " - " + request.getDescription()
                : "";

        // Record debit transaction
        Transaction debitTx = new Transaction(
                sourceAccount,
                TransactionType.TRANSFER_DEBIT,
                request.getAmount(),
                sourceAccount.getBalance(),
                "Transfer to " + destinationAccount.getAccountNumber() + note,
                transferRef,
                destinationAccount.getAccountNumber()
        );

        // Record credit transaction
        Transaction creditTx = new Transaction(
                destinationAccount,
                TransactionType.TRANSFER_CREDIT,
                request.getAmount(),
                destinationAccount.getBalance(),
                "Transfer from " + sourceAccount.getAccountNumber() + note,
                transferRef,
                sourceAccount.getAccountNumber()
        );

        Transaction savedDebitTx = transactionRepository.save(debitTx);
        transactionRepository.save(creditTx);

        return accountService.mapToTransactionResponse(savedDebitTx);
    }
}

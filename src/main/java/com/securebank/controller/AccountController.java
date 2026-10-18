package com.securebank.controller;

import com.securebank.dto.AccountResponse;
import com.securebank.dto.AccountStatementResponse;
import com.securebank.dto.CreateAccountRequest;
import com.securebank.dto.TransactionResponse;
import com.securebank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse response = accountService.createAccount(request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<AccountResponse> accounts = accountService.getAccountsForUser(userDetails.getUsername());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse response = accountService.getAccountById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TransactionResponse> txs = accountService.getRecentTransactions(id, userDetails.getUsername(), limit);
        return ResponseEntity.ok(txs);
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<AccountStatementResponse> getStatement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        AccountStatementResponse statement = accountService.getAccountStatement(id, userDetails.getUsername());
        return ResponseEntity.ok(statement);
    }

    @PostMapping("/{id}/apply-interest")
    public ResponseEntity<TransactionResponse> applyInterest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = accountService.applyInterest(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}

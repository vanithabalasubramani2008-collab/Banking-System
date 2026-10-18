package com.securebank.controller;

import com.securebank.dto.DepositRequest;
import com.securebank.dto.TransactionResponse;
import com.securebank.dto.TransferRequest;
import com.securebank.dto.WithdrawalRequest;
import com.securebank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/accounts/{id}/deposits")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = transactionService.deposit(id, request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = transactionService.withdraw(id, request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = transactionService.transfer(request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}

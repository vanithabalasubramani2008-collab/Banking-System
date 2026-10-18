package com.securebank.dto;

import com.securebank.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
    private AccountType accountType;

    @NotNull(message = "Initial deposit amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    @NotBlank(message = "PIN is required to authorize account opening")
    private String pin;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(AccountType accountType, BigDecimal initialDeposit, String pin) {
        this.accountType = accountType;
        this.initialDeposit = initialDeposit;
        this.pin = pin;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}

package com.securebank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class WithdrawalRequest {

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Withdrawal amount must be strictly greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "PIN is required to authorize withdrawal")
    private String pin;

    private String description;

    public WithdrawalRequest() {
    }

    public WithdrawalRequest(BigDecimal amount, String pin, String description) {
        this.amount = amount;
        this.pin = pin;
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

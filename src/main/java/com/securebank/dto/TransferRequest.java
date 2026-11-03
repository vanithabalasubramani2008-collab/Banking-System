package com.securebank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    @Size(max = 20, message = "Source account number must be at most 20 characters")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    @Size(max = 20, message = "Destination account number must be at most 20 characters")
    private String destinationAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Transfer amount must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Transfer amount must have at most 13 integer and 2 fractional digits")
    private BigDecimal amount;

    @NotBlank(message = "PIN is required to authorize transfer")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be a 4 to 6 digit numerical code")
    private String pin;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    public TransferRequest() {
    }

    public TransferRequest(String sourceAccountNumber, String destinationAccountNumber,
                           BigDecimal amount, String pin, String description) {
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.pin = pin;
        this.description = description;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
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

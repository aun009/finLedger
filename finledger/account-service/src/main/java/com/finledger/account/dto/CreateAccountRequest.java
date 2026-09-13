package com.finledger.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "ownerName is required")
        String ownerName,

        @DecimalMin(value = "0.00", message = "initialBalance cannot be negative")
        BigDecimal initialBalance
) {
}

package com.banking.dto;

import com.banking.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotNull(message = "Account type is required (SAVINGS or CHECKING)")
    private AccountType accountType;

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;
}

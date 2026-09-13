package com.banking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum loan amount is 1,000.00")
    @DecimalMax(value = "5000000.0", message = "Maximum loan amount is 5,000,000.00")
    private BigDecimal amount;

    @NotNull(message = "Tenure in months is required")
    @Min(value = 3, message = "Minimum tenure is 3 months")
    @Max(value = 360, message = "Maximum tenure is 360 months (30 years)")
    private Integer tenureMonths;

    private String remarks;
}

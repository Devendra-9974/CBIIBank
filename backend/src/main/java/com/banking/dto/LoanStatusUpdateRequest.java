package com.banking.dto;

import com.banking.enums.LoanStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanStatusUpdateRequest {

    @NotNull(message = "Loan status is required (APPROVED or REJECTED)")
    private LoanStatus status;

    private String remarks;
}

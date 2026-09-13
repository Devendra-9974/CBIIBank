package com.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryRequest {

    @NotBlank(message = "Beneficiary name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String beneficiaryName;

    @NotBlank(message = "Account number is required")
    @Size(min = 8, max = 20, message = "Account number must be between 8 and 20 characters")
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC Code format (e.g., HDFC0001234)")
    private String ifscCode;
}

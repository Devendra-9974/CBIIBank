package com.banking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryResponse {
    private Long id;
    private String beneficiaryName;
    private String accountNumber;
    private String bankName;
    private String ifscCode;
    private LocalDateTime createdAt;
}

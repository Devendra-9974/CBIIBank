package com.banking.dto;

import com.banking.enums.LoanStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private LoanStatus status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

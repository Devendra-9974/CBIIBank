package com.banking.dto;

import com.banking.enums.AccountStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    private Long accountId;
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;
}
